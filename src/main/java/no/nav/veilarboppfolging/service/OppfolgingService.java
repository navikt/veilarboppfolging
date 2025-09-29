package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ArenaSyncRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AdminAvregistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering;
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.*;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.EnumUtils;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.ArenaUtils.kanSettesUnderOppfolging;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
public class OppfolgingService {

    private final KafkaProducerService kafkaProducerService;
    private final KvpService kvpService;
    private final ArenaOppfolgingService arenaOppfolgingService;
    private final AuthService authService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final ManuellStatusService manuellStatusService;

    private final AmtDeltakerClient amtDeltakerClient;

    private final KvpRepository kvpRepository;
    private final MaalRepository maalRepository;
    private final BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository;
    private final TransactionTemplate transactor;
    private final ArenaYtelserService arenaYtelserService;
    private final BigQueryClient bigQueryClient;

    @Autowired
    public OppfolgingService(
            KafkaProducerService kafkaProducerService,
            KvpService kvpService,
            ArenaOppfolgingService arenaOppfolgingService,
            AuthService authService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            // TODO: Når vi får splittet servicenen bedre så skal det ikke være behov for å bruke @Lazy
            @Lazy ManuellStatusService manuellStatusService,
            AmtDeltakerClient amtDeltakerClient,
            KvpRepository kvpRepository,
            MaalRepository maalRepository,
            BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository,
            TransactionTemplate transactor,
            ArenaYtelserService arenaYtelserService,
            BigQueryClient bigQueryClient,
            @Value("${app.env.nav-no-url}") String navNoUrl) {
        this.kafkaProducerService = kafkaProducerService;
        this.kvpService = kvpService;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.authService = authService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.manuellStatusService = manuellStatusService;
        this.amtDeltakerClient = amtDeltakerClient;
        this.kvpRepository = kvpRepository;
        this.maalRepository = maalRepository;
        this.brukerOppslagFlereOppfolgingAktorRepository = brukerOppslagFlereOppfolgingAktorRepository;
        this.transactor = transactor;
        this.arenaYtelserService = arenaYtelserService;
        this.bigQueryClient = bigQueryClient;
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
    public AvslutningStatusData avsluttOppfolging(Avregistrering avregistrering) {
        AktorId aktorId = avregistrering.getAktorId();
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
                .orElseThrow(() -> new RuntimeException("Feilet under henting av oppfolgingsstatus (db) med fallback til /oppfolgingsbruker"));

        if (authService.erInternBruker()) {
            authService.sjekkSkriveTilgangMedFnr(fnr);
            authService.sjekkTilgangTilEnhet(arenaOppfolgingTilstand.getOppfolgingsenhet());
            secureLog.info("Veileder: {} forsøker å avslutte oppfølging for fnr: {}", authService.getInnloggetBrukerIdent(), fnr.get());
        } else {
            secureLog.info("Forsøker å avslutte oppfølging for fnr: {} som systembruker", fnr.get());
        }

        boolean erIserv = erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, arenaOppfolgingTilstand.getFormidlingsgruppe()));

        boolean harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser(fnr);
        boolean underKvp = kvpService.erUnderKvp(aktorId);
        if (kanAvslutteOppfolging(aktorId, erUnderOppfolging(aktorId), erIserv, harAktiveTiltaksdeltakelser, underKvp)) {
            var veilederId = avregistrering.getAvsluttetAv().getIdent();
            var begrunnelse = avregistrering.getBegrunnelse();
            secureLog.info("Avslutting av oppfølging utført av: {}, begrunnelse: {}, tilstand i Arena for aktorid {}: {}", veilederId, begrunnelse, aktorId, arenaOppfolgingTilstand);
            avsluttOppfolgingForBruker(avregistrering);
        } else {
            log.warn("Oppfølging ble ikke avsluttet likevel");
        }

        return getAvslutningStatus(fnr);
    }

    @SneakyThrows
    public VeilederTilgang hentVeilederTilgang(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return Optional.ofNullable(arenaOppfolgingService.hentArenaOppfolgingsEnhetId(fnr))
                .map(Id::get)
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

        Optional<KvpPeriodeEntity> maybeKvpPeriode = empty();

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

    private Optional<Kvalifiseringsgruppe> getKvalifiseringsGruppe(OppfolgingsRegistrering oppfolgingsbruker) {
        if (oppfolgingsbruker instanceof ArenaSyncRegistrering arenasyncoppfolgingsbruker) {
            return Optional.ofNullable(arenasyncoppfolgingsbruker.getKvalifiseringsgruppe());
        } else {
            return Optional.empty();
        }
    }

    private boolean kanAvslutteOppfolging(AktorId aktorId, boolean erUnderOppfolging, boolean erIservIArena, boolean harAktiveTiltaksdeltakelser, boolean underKvp) {
        secureLog.info("Kan oppfolging avsluttes for aktorid {}?, oppfolging.isUnderOppfolging(): {}, erIservIArena(): {}, underKvp(): {}, harAktiveTiltaksdeltakelser(): {}",
                aktorId, erUnderOppfolging, erIservIArena, underKvp, harAktiveTiltaksdeltakelser);

        return erUnderOppfolging
                && erIservIArena
                && !underKvp
                && !harAktiveTiltaksdeltakelser;
    }

    public void adminForceAvsluttOppfolgingForBruker(AktorId aktorId, String veilederId, String begrunnelse) {
        avsluttOppfolgingForBruker(new AdminAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(veilederId)), begrunnelse, null));
    }

    private void avsluttOppfolgingForBruker(Avregistrering avregistrering) {
        var fnr = authService.getFnrOrThrow(avregistrering.getAktorId());
        var aktorId = avregistrering.getAktorId();
        transactor.executeWithoutResult((ignored) -> {
            oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(aktorId, avregistrering.getAvsluttetAv().getIdent(), avregistrering.getBegrunnelse());

            var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
            var sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

            log.info("Oppfølgingsperiode avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.");
            kafkaProducerService.publiserOppfolgingsperiode(DtoMappers.tilOppfolgingsperiodeDTO(sistePeriode));
            kafkaProducerService.publiserVeilederTilordnet(aktorId, null, null);
            kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, false);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, false);
            kafkaProducerService.publiserOppfolgingsAvsluttet(OppfolgingsAvsluttetHendelseDto.Companion.of(avregistrering, sistePeriode, fnr));
            kafkaProducerService.publiserSkjulAoMinSideMicrofrontend(aktorId, fnr);

            bigQueryClient.loggAvsluttOppfolgingsperiode(sistePeriode.getUuid(), avregistrering.getAvregistreringsType());
        });
    }

    public void adminAvsluttSpesifikkOppfolgingsperiode(AktorId aktorId, String veilederId, String begrunnelse, String uuid) {
        if (uuid == null) {
            log.info("oppfolgingsperiodeUUID er null");
            return;
        }

        try {
            UUID oppfolgingsperiodeUUID = UUID.fromString(uuid);
            avsluttValgtOppfolgingsperiode(new AdminAvregistrering(aktorId, new VeilederRegistrant(new NavIdent(veilederId)), begrunnelse, oppfolgingsperiodeUUID));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for oppfolgingsperiodeUUID: {}", uuid, e);
        }
    }

    private void avsluttValgtOppfolgingsperiode(AdminAvregistrering avregistrering) {
        var oppfolgingsperiodeUUID = avregistrering.getOppfolgingsperiodeUUID();
        var gjeldendePerioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(avregistrering.getAktorId()).stream().filter(p -> p.getSluttDato() == null).toList();
        var sisteGjeldendePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(gjeldendePerioder);
        var valgtGjeldendePeriode = gjeldendePerioder.stream().filter(p -> p.getUuid().equals(oppfolgingsperiodeUUID)).findFirst().orElse(null);

        if (valgtGjeldendePeriode == null) {
            log.warn("Fant ikke oppfølgingsperiode med UUID: {}. (eller den er allerede avsluttet)", oppfolgingsperiodeUUID);
            return;
        }

        boolean erSisteGjeldendePeriode = valgtGjeldendePeriode.getUuid().equals(sisteGjeldendePeriode.getUuid());
        boolean erEnesteGjeldendePeriode = gjeldendePerioder.size() == 1;

        if (erSisteGjeldendePeriode && erEnesteGjeldendePeriode) {
            log.info("Valgt oppfølgingsperiode er siste og eneste. Avslutter oppfølging.");
            avsluttOppfolgingForBruker(avregistrering);
            return;
        }

        var sluttDato = erSisteGjeldendePeriode ? now() : sisteGjeldendePeriode.getStartDato();
        var avsluttetOppfolgingsperiode = oppfolgingsPeriodeRepository.avsluttOppfolgingsperiode(oppfolgingsperiodeUUID, avregistrering.getAvsluttetAv().getIdent(), avregistrering.getBegrunnelse(), sluttDato);

        log.info("Oppfølgingsperiode med UUID: {} avsluttet for bruker - publiserer endringer på oppfølgingsperiode-topics.", oppfolgingsperiodeUUID);
        kafkaProducerService.publiserValgtOppfolgingsperiode(DtoMappers.tilOppfolgingsperiodeDTO(avsluttetOppfolgingsperiode));
        bigQueryClient.loggAvsluttOppfolgingsperiode(oppfolgingsperiodeUUID, avregistrering.getAvregistreringsType());
    }

    public boolean erUnderOppfolging(AktorId aktorId) {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .map(OppfolgingEntity::isUnderOppfolging)
                .orElse(false);
    }

    protected boolean harAktiveTiltaksdeltakelser(Fnr fnr) {
        return amtDeltakerClient.harAktiveTiltaksdeltakelser(fnr.get());
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
        Optional<VeilarbArenaOppfolgingsStatus> maybeArenaOppfolging = arenaOppfolgingService.hentArenaOppfolgingsStatus(fnr);

        boolean kanSettesUnderOppfolging = !oppfolging.isUnderOppfolging() && maybeArenaOppfolging
                .map(s -> kanSettesUnderOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, s.getFormidlingsgruppe()), EnumUtils.valueOf(Kvalifiseringsgruppe.class, s.getServicegruppe())))
                .orElse(false);

        long kvpId = kvpRepository.gjeldendeKvp(aktorId);
        boolean harSkrivetilgangTilBruker = !kvpService.erUnderKvp(kvpId)
                || authService.harTilgangTilEnhet(
                kvpRepository.hentKvpPeriode(kvpId)
                        .orElseThrow()
                        .getEnhet()
        );

        Boolean erInaktivIArena = maybeArenaOppfolging.map(ao -> erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()))).orElse(null);

        Optional<Boolean> maybeKanEnkeltReaktiveres = maybeArenaOppfolging
                .flatMap((it) -> Optional.ofNullable(it.getKanEnkeltReaktiveres()));

        Boolean kanReaktiveres = maybeKanEnkeltReaktiveres
                .map(kr -> oppfolging.isUnderOppfolging() && kr)
                .orElse(null);

        Boolean erSykmeldtMedArbeidsgiver = maybeArenaOppfolging
                .map(ao -> ArenaUtils.erIARBSUtenOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()), EnumUtils.valueOf(Kvalifiseringsgruppe.class, ao.getServicegruppe())))
                .orElse(null);

        LocalDate inaktiveringsDato = maybeArenaOppfolging
                .map(VeilarbArenaOppfolgingsStatus::getInaktiveringsdato)
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
                .setServicegruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getServicegruppe).orElse(null))
                .setFormidlingsgruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getFormidlingsgruppe).orElse(null))
                .setRettighetsgruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getRettighetsgruppe).orElse(null))
                .setKanVarsles(!erManuell && digdirKontaktinfo.isKanVarsles());
    }

    private AvslutningStatusData getAvslutningStatus(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr);

        boolean erIserv = maybeArenaOppfolging.map(ao -> erIserv(EnumUtils.valueOf(Formidlingsgruppe.class, ao.getFormidlingsgruppe()))).orElse(false);

        boolean harAktiveTiltaksdeltakelser = harAktiveTiltaksdeltakelser(fnr);
        boolean underKvp = kvpService.erUnderKvp(aktorId);
        boolean kanAvslutte = kanAvslutteOppfolging(aktorId, erUnderOppfolging(aktorId), erIserv, harAktiveTiltaksdeltakelser, underKvp);

        boolean erUnderOppfolgingIArena = maybeArenaOppfolging
                .map(status -> ArenaUtils.erUnderOppfolging(EnumUtils.valueOf(Formidlingsgruppe.class, status.getFormidlingsgruppe()), EnumUtils.valueOf(Kvalifiseringsgruppe.class, status.getServicegruppe())))
                .orElse(false);

        LocalDate inaktiveringsDato = maybeArenaOppfolging
                .map(ArenaOppfolgingTilstand::getInaktiveringsdato)
                .orElse(null);

        return AvslutningStatusData.builder()
                .kanAvslutte(kanAvslutte)
                .underOppfolging(erUnderOppfolgingIArena)
                .harYtelser(arenaYtelserService.harPagaendeYtelse(fnr))
                .underKvp(kvpService.erUnderKvp(aktorId))
                .inaktiveringsDato(inaktiveringsDato)
                .erIserv(erIserv)
                .harAktiveTiltaksdeltakelser(harAktiveTiltaksdeltakelser)
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

    public void oppdaterArenaOppfolgingStatus(AktorId aktorId, Boolean skalOppretteOppfolgingForst, LocalArenaOppfolging arenaOppfolging) {
        oppfolgingsStatusRepository.oppdaterArenaOppfolgingStatus(
                aktorId,
                skalOppretteOppfolgingForst,
                arenaOppfolging
        );
    }
}
