package no.nav.veilarboppfolging.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.*;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;


@Service
public class OppfolgingService {

    private final KvpService kvpService;
    private final ArenaOppfolgingService arenaOppfolgingService;
    private final AuthService authService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final ManuellStatusService manuellStatusService;
    private final ArbeidsoppfolgingsKontorService arbeidsoppfolgingsKontorService;

    private final TiltakshistorikkClient tiltakshistorikkClient;

    private final KvpRepository kvpRepository;
    private final MaalRepository maalRepository;
    private final BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository;

    @Autowired
    public OppfolgingService(
            KvpService kvpService,
            ArenaOppfolgingService arenaOppfolgingService,
            AuthService authService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            // TODO: Når vi får splittet servicenen bedre så skal det ikke være behov for å bruke @Lazy
            @Lazy ManuellStatusService manuellStatusService,
            KvpRepository kvpRepository,
            MaalRepository maalRepository,
            BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository,
            ArbeidsoppfolgingsKontorService arbeidsoppfolgingsKontorService,
            TiltakshistorikkClient tiltakshistorikkClient) {
        this.kvpService = kvpService;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.authService = authService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.manuellStatusService = manuellStatusService;
        this.kvpRepository = kvpRepository;
        this.maalRepository = maalRepository;
        this.brukerOppslagFlereOppfolgingAktorRepository = brukerOppslagFlereOppfolgingAktorRepository;
        this.arbeidsoppfolgingsKontorService = arbeidsoppfolgingsKontorService;
        this.tiltakshistorikkClient = tiltakshistorikkClient;
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

    
    public VeilederTilgang hentVeilederTilgang(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return Optional.ofNullable(arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr))
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

    public boolean erUnderOppfolging(AktorId aktorId) {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .map(OppfolgingEntity::isUnderOppfolging)
                .orElse(false);
    }

    public boolean harAktiveTiltaksdeltakelser(Fnr fnr) {
        return tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get());
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

        boolean harSkrivetilgangTilBruker = harVeilederTilgangTilKontorsperretEnhet(aktorId);

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
                .setKanStarteOppfolging(!oppfolging.isUnderOppfolging())
                .setOppfolgingsperioder(oppfolging.getOppfolgingsperioder())
                .setHarSkriveTilgang(harSkrivetilgangTilBruker)
                .setInaktivIArena(erInaktivIArena)
                .setKanReaktiveres(kanReaktiveres)
                // Usikker på om dette feltet er i bruk av konsumenter
                .setErSykmeldtMedArbeidsgiver(erSykmeldtMedArbeidsgiver)
                .setInaktiveringsdato(inaktiveringsDato)
                .setServicegruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getServicegruppe).orElse(null))
                .setFormidlingsgruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getFormidlingsgruppe).orElse(null))
                .setRettighetsgruppe(maybeArenaOppfolging.map(VeilarbArenaOppfolgingsStatus::getRettighetsgruppe).orElse(null))
                .setKanVarsles(!erManuell && digdirKontaktinfo.isKanVarsles());
    }

    public Boolean harVeilederTilgangTilKontorsperretEnhet(AktorId aktorId) {
        long kvpId = kvpRepository.gjeldendeKvp(aktorId);
        boolean brukerErUtenKontorSperre = !kvpService.erUnderKvp(kvpId);
        return brukerErUtenKontorSperre || authService.harTilgangTilEnhet(
                kvpRepository.hentKvpPeriode(kvpId)
                        .orElseThrow()
                        .getEnhet()
        );
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
