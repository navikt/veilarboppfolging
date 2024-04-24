package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.*;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.EnumUtils;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.utils.ArenaUtils.*;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
public class OppfolgingService {

    private final KafkaProducerService kafkaProducerService;
    private final YtelserOgAktiviteterService ytelserOgAktiviteterService;
    private final KvpService kvpService;
    private final MetricsService metricsService;
    private final ArenaOppfolgingService arenaOppfolgingService;
    private final AuthService authService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final ManuellStatusService manuellStatusService;

    private final KvpRepository kvpRepository;
    private final MaalRepository maalRepository;
    private final BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository;
    private final TransactionTemplate transactor;

    @Autowired
    public OppfolgingService(
            KafkaProducerService kafkaProducerService,
            YtelserOgAktiviteterService ytelserOgAktiviteterService,
            KvpService kvpService,
            MetricsService metricsService,
            ArenaOppfolgingService arenaOppfolgingService,
            AuthService authService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            // TODO: Når vi får splittet servicenen bedre så skal det ikke være behov for å bruke @Lazy
            @Lazy ManuellStatusService manuellStatusService,
            KvpRepository kvpRepository,
            MaalRepository maalRepository,
            BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository,
            TransactionTemplate transactor
    ) {
        this.kafkaProducerService = kafkaProducerService;
        this.ytelserOgAktiviteterService = ytelserOgAktiviteterService;
        this.kvpService = kvpService;
        this.metricsService = metricsService;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.authService = authService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.manuellStatusService = manuellStatusService;
        this.kvpRepository = kvpRepository;
        this.maalRepository = maalRepository;
        this.brukerOppslagFlereOppfolgingAktorRepository = brukerOppslagFlereOppfolgingAktorRepository;
        this.transactor = transactor;
    }

    @Transactional // TODO: kan denne være read only?
    public OppfolgingStatusData hentOppfolgingsStatus(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return getOppfolgingStatusData(fnr);
    }

    private List<AktorId> hentAktorIderMedOppfolging(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        var aktorIder = authService.getAlleAktorIderOrThrow(fnr);
        return aktorIder
                .stream()
                .filter(aktorId -> !oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).isEmpty())
                .collect(toList());

    }

    public boolean hentHarFlereAktorIderMedOppfolging(Fnr fnr) {
        boolean harFlereAktorIdMedOppfolging = hentAktorIderMedOppfolging(fnr).size() > 1;

        if (harFlereAktorIdMedOppfolging) {
            brukerOppslagFlereOppfolgingAktorRepository.insertBrukerHvisNy(fnr);
        }

        return harFlereAktorIdMedOppfolging;
    }

    public AvslutningStatusData hentAvslutningStatus(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return getAvslutningStatus(fnr);
    }

    @SneakyThrows
    public AvslutningStatusData avsluttOppfolging(Fnr fnr, String veilederId, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedFnr(fnr);

        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        authService.sjekkTilgangTilEnhet(arenaOppfolgingTilstand.getOppfolgingsenhet());

        boolean erIserv = erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, arenaOppfolgingTilstand.getFormidlingsgruppe()));

        if (kanAvslutteOppfolging(aktorId, erUnderOppfolging(aktorId), erIserv)) {
            secureLog.info("Avslutting av oppfølging, tilstand i Arena for aktorid {}: {}", aktorId, arenaOppfolgingTilstand);
            avsluttOppfolgingForBruker(aktorId, veilederId, begrunnelse);
        }

        return getAvslutningStatus(fnr);
    }

    public boolean avsluttOppfolgingForSystemBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr)
                .orElseThrow();

        secureLog.info("Avslutting av oppfølging, tilstand i Arena for aktorid {}: {}", aktorId, arenaOppfolgingTilstand);

        boolean erIserv = erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, arenaOppfolgingTilstand.getFormidlingsgruppe()));

        if (!kanAvslutteOppfolging(aktorId, erUnderOppfolging(aktorId), erIserv)) {
            return false;
        }

        avsluttOppfolgingForBruker(aktorId, SYSTEM_USER_NAME, "Oppfølging avsluttet automatisk grunnet iserv i 28 dager");
        return true;
    }

    @SneakyThrows
    public VeilederTilgang hentVeilederTilgang(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return arenaOppfolgingService
                .hentOppfolgingFraVeilarbarena(fnr)
                .map(VeilarbArenaOppfolging::getNav_kontor)
                .map(authService::harTilgangTilEnhet)
                .map(VeilederTilgang::new)
                .orElse(new VeilederTilgang(false));
    }

    public List<OppfolgingsperiodeEntity> hentOppfolgingsperioder(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return hentOppfolgingsperioder(aktorId);
    }

    public List<OppfolgingsperiodeEntity> hentOppfolgingsperioder(AktorId aktorId) {
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
    }

    public UnderOppfolgingDTO oppfolgingData(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedFnr(fnr);

        return getOppfolgingStatus(fnr)
                .map(oppfolgingsstatus -> {
                    boolean isUnderOppfolging = oppfolgingsstatus.isUnderOppfolging();
                    boolean erManuell = manuellStatusService.erManuell(aktorId);

                    return new UnderOppfolgingDTO()
                            .setUnderOppfolging(isUnderOppfolging)
                            .setErManuell(isUnderOppfolging && erManuell);
                })
                .orElse(new UnderOppfolgingDTO().setUnderOppfolging(false).setErManuell(false));
    }

    public boolean erUnderOppfolgingNiva3(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkTilgangTilPersonMedNiva3(aktorId);

        return erUnderOppfolging(aktorId);
    }

    public boolean erUnderOppfolging(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return erUnderOppfolging(aktorId);
    }

    public Optional<OppfolgingsperiodeEntity> hentOppfolgingsperiode(String uuid) {
        return oppfolgingsPeriodeRepository.hentOppfolgingsperiode(uuid);
    }

    @SneakyThrows
    public Optional<Oppfolging> hentOppfolging(AktorId aktorId) {
        Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        if (maybeOppfolging.isEmpty()) {
            return empty();
        }

        OppfolgingEntity oppfolgingEntity = maybeOppfolging.get();

        Oppfolging oppfolging = new Oppfolging()
                .setAktorId(oppfolgingEntity.getAktorId())
                .setVeilederId(oppfolgingEntity.getVeilederId())
                .setUnderOppfolging(oppfolgingEntity.isUnderOppfolging());

        Optional<KvpPeriodeEntity> maybeKvpPeriode= empty();

        if (oppfolgingEntity.getGjeldendeKvpId() != 0) {
            maybeKvpPeriode = kvpRepository.hentKvpPeriode(oppfolgingEntity.getGjeldendeKvpId());

            maybeKvpPeriode.ifPresentOrElse((kvpPeriode) -> {
                if (authService.harTilgangTilEnhet(kvpPeriode.getEnhet())) {
                    oppfolging.setGjeldendeKvp(kvpPeriode);
                }
            }, () -> log.error("Fant ikke KVP periode for id " + oppfolgingEntity.getGjeldendeKvpId()));
        }

        if (oppfolgingEntity.getGjeldendeMaalId() != 0) {
            Optional<MaalEntity> maybeMaal = maalRepository.hentMaal(oppfolgingEntity.getGjeldendeMaalId());

            maybeMaal.ifPresentOrElse(
                    oppfolging::setGjeldendeMal,
                    () -> log.error("Fant ikke maal for id " + oppfolgingEntity.getGjeldendeMaalId())
            );
        }

        Optional<ManuellStatusEntity> manuellStatus = manuellStatusService.hentManuellStatus(aktorId);
        manuellStatus.ifPresent(oppfolging::setGjeldendeManuellStatus);

        List<KvpPeriodeEntity> kvpPerioder = kvpRepository.hentKvpHistorikk(aktorId);
        oppfolging.setOppfolgingsperioder(populerKvpPerioder(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AktorId.of(oppfolgingEntity.getAktorId())), kvpPerioder));

        return Optional.of(oppfolging);
    }

    public void startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker oppfolgingsbruker) {
        AktorId aktorId = AktorId.of(oppfolgingsbruker.getAktoerId());
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        KRRData kontaktinfo = manuellStatusService.hentDigdirKontaktinfo(fnr);

        transactor.executeWithoutResult((ignored) -> {
            Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

            boolean erUnderOppfolging = maybeOppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);

            if (erUnderOppfolging) {
                return;
            }

            if (maybeOppfolging.isEmpty()) {
                // Siden det blir gjort mange kall samtidig til flere noder kan det oppstå en race condition
                // hvor oppfølging har blitt insertet av en annen node etter at den har sjekket at oppfølging
                // ikke ligger i databasen.
                try {
                    oppfolgingsStatusRepository.opprettOppfolging(aktorId);
                } catch (DuplicateKeyException e) {
                    secureLog.warn("Race condition oppstod under oppretting av ny oppfølging for bruker: " + aktorId);
                    return;
                }
            }

            oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartBegrunnelse());

            List<OppfolgingsperiodeEntity> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
            OppfolgingsperiodeEntity sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

            log.info("Oppfølgingsperiode startet for bruker - publiserer endringer på oppfølgingsperiode-topics.");
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilSisteOppfolgingsperiodeV1(sistePeriode));

            if (kontaktinfo.isReservert()) {
                manuellStatusService.settBrukerTilManuellGrunnetReservertIKRR(aktorId);
            }
        });
    }

    public boolean kanAvslutteOppfolging(AktorId aktorId, boolean erUnderOppfolging, boolean erIservIArena) {
        boolean ikkeUnderKvp = !kvpService.erUnderKvp(aktorId);

        secureLog.info("Kan oppfolging avsluttes for aktorid {}?, oppfolging.isUnderOppfolging(): {}, erIservIArena(): {}, !erUnderKvp(): {}",
                aktorId, erUnderOppfolging, erIservIArena, ikkeUnderKvp);

        return erUnderOppfolging
                && erIservIArena
                && ikkeUnderKvp;
    }

    public void avsluttOppfolgingForBruker(AktorId aktorId, String veilederId, String begrunnelse) {
        transactor.executeWithoutResult((ignored) -> {

            oppfolgingsPeriodeRepository.avslutt(aktorId, veilederId, begrunnelse);

            List<OppfolgingsperiodeEntity> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
            OppfolgingsperiodeEntity sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

            log.info("Oppfølgingsperiode avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.");
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilSisteOppfolgingsperiodeV1(sistePeriode));

            // Publiserer også endringer som resettes i oppfolgingsstatus-tabellen ved avslutting av oppfølging
            kafkaProducerService.publiserVeilederTilordnet(aktorId, null);
            kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, false);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, false);
        });
    }

    protected boolean erUnderOppfolging(AktorId aktorId) {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .map(OppfolgingEntity::isUnderOppfolging)
                .orElse(false);
    }

    private Optional<OppfolgingEntity> getOppfolgingStatus(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);
        return oppfolgingsStatusRepository.hentOppfolging(aktorId);
    }

    private OppfolgingStatusData getOppfolgingStatusData(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        Oppfolging oppfolging = hentOppfolging(aktorId)
                .orElse(new Oppfolging().setAktorId(aktorId.get()).setUnderOppfolging(false));

        boolean erManuell = manuellStatusService.erManuell(aktorId);

        KRRData digdirKontaktinfo = manuellStatusService.hentDigdirKontaktinfo(fnr);

        // TODO: Burde kanskje heller feile istedenfor å bruke Optional
        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = arenaOppfolgingService.hentOppfolgingTilstand(fnr);

        boolean kanSettesUnderOppfolging = !oppfolging.isUnderOppfolging() && maybeArenaOppfolging
                .map(s -> kanSettesUnderOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, s.getFormidlingsgruppe()), EnumUtils.valueOf(Kvalifiseringsgruppe.class, s.getServicegruppe()) ))
                .orElse(false);

        long kvpId = kvpRepository.gjeldendeKvp(aktorId);
        boolean harSkrivetilgangTilBruker = !kvpService.erUnderKvp(kvpId)
                || authService.harTilgangTilEnhet(
                kvpRepository.hentKvpPeriode(kvpId)
                        .orElseThrow()
                        .getEnhet()
        );

        Boolean erInaktivIArena = maybeArenaOppfolging.map(ao -> erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()))).orElse(null);

        Optional<Boolean> maybeKanEnkeltReaktiveres = maybeArenaOppfolging.map(ArenaOppfolgingTilstand::getKanEnkeltReaktiveres);

        Boolean kanReaktiveres = maybeKanEnkeltReaktiveres
                .map(kr -> oppfolging.isUnderOppfolging() && kr)
                .orElse(null);

        Boolean erSykmeldtMedArbeidsgiver = maybeArenaOppfolging
                .map(ao -> ArenaUtils.erIARBSUtenOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()) ,EnumUtils.valueOf(Kvalifiseringsgruppe.class,  ao.getServicegruppe())))
                .orElse(null);

        LocalDate inaktiveringsDato = maybeArenaOppfolging
                .map(ArenaOppfolgingTilstand::getInaktiveringsdato)
                .orElse(null);

        return new OppfolgingStatusData()
                .setFnr(fnr.get())
                .setAktorId(oppfolging.getAktorId())
                .setVeilederId(oppfolging.getVeilederId())
                .setUnderOppfolging(oppfolging.isUnderOppfolging())
                .setUnderKvp(oppfolging.getGjeldendeKvp() != null)
                .setReservasjonKRR(digdirKontaktinfo.isReservert())
                .setRegistrertKRR(digdirKontaktinfo.isAktiv())
                .setManuell(erManuell || digdirKontaktinfo.isReservert())
                .setKanStarteOppfolging(kanSettesUnderOppfolging)
                .setOppfolgingsperioder(oppfolging.getOppfolgingsperioder())
                .setHarSkriveTilgang(harSkrivetilgangTilBruker)
                .setInaktivIArena(erInaktivIArena)
                .setKanReaktiveres(kanReaktiveres)
                .setErSykmeldtMedArbeidsgiver(erSykmeldtMedArbeidsgiver)
                .setErIkkeArbeidssokerUtenOppfolging(erSykmeldtMedArbeidsgiver)
                .setInaktiveringsdato(inaktiveringsDato)
                .setServicegruppe(maybeArenaOppfolging.map(ArenaOppfolgingTilstand::getServicegruppe).orElse(null))
                .setFormidlingsgruppe(maybeArenaOppfolging.map(ArenaOppfolgingTilstand::getFormidlingsgruppe).orElse(null))
                .setRettighetsgruppe(maybeArenaOppfolging.map(ArenaOppfolgingTilstand::getRettighetsgruppe).orElse(null))
                .setKanVarsles(!erManuell && digdirKontaktinfo.isKanVarsles());
    }

    private AvslutningStatusData getAvslutningStatus(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = arenaOppfolgingService.hentOppfolgingTilstand(fnr);

        boolean erIserv = maybeArenaOppfolging.map(ao -> erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()))).orElse(false);

        boolean kanAvslutte = kanAvslutteOppfolging(aktorId, erUnderOppfolging(aktorId), erIserv);

        boolean erUnderOppfolgingIArena = maybeArenaOppfolging
                .map(status -> ArenaUtils.erUnderOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, status.getFormidlingsgruppe()) , EnumUtils.valueOf(Kvalifiseringsgruppe.class, status.getServicegruppe()) ))
                .orElse(false);

        LocalDate inaktiveringsDato = maybeArenaOppfolging
                .map(ArenaOppfolgingTilstand::getInaktiveringsdato)
                .orElse(null);

        return AvslutningStatusData.builder()
                .kanAvslutte(kanAvslutte)
                .underOppfolging(erUnderOppfolgingIArena)
                .harYtelser(ytelserOgAktiviteterService.harPagaendeYtelse(fnr))
                .underKvp(kvpService.erUnderKvp(aktorId))
                .inaktiveringsDato(inaktiveringsDato)
                .erIserv(erIserv)
                .build();
    }

    private List<OppfolgingsperiodeEntity> populerKvpPerioder(List<OppfolgingsperiodeEntity> oppfolgingsPerioder, List<KvpPeriodeEntity> kvpPerioder) {
        return oppfolgingsPerioder.stream()
                .map(periode -> periode.toBuilder().kvpPerioder(
                        kvpPerioder.stream()
                                .filter(kvp -> authService.harTilgangTilEnhetMedSperre(kvp.getEnhet()))
                                .filter(kvp -> erKvpIPeriode(kvp, periode))
                                .collect(toList())
                ).build())
                .collect(toList());
    }

    private boolean erKvpIPeriode(KvpPeriodeEntity kvp, OppfolgingsperiodeEntity periode) {
        return kvpEtterStartenAvPeriode(kvp, periode)
                && kvpForSluttenAvPeriode(kvp, periode);
    }

    private boolean kvpEtterStartenAvPeriode(KvpPeriodeEntity kvp, OppfolgingsperiodeEntity periode) {
        return !periode.getStartDato().isAfter(kvp.getOpprettetDato());
    }

    private boolean kvpForSluttenAvPeriode(KvpPeriodeEntity kvp, OppfolgingsperiodeEntity periode) {
        return periode.getSluttDato() == null || !periode.getSluttDato().isBefore(kvp.getOpprettetDato());
    }

    public Optional<OppfolgingsperiodeEntity> hentGjeldendeOppfolgingsperiode(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
    }
}
