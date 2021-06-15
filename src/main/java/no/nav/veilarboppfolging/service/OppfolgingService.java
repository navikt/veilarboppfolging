package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static no.nav.veilarboppfolging.utils.ArenaUtils.*;
import static no.nav.veilarboppfolging.utils.KvpUtils.sjekkTilgangGittKvp;

@Slf4j
@Service
public class OppfolgingService {

    private final KafkaProducerService kafkaProducerService;
    private final YtelserOgAktiviteterService ytelserOgAktiviteterService;
    private final DkifClient dkifClient;
    private final KvpService kvpService;
    private final MetricsService metricsService;
    private final ArenaOppfolgingService arenaOppfolgingService;
    private final AuthService authService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private final ManuellStatusRepository manuellStatusRepository;
    private final ManuellStatusService manuellStatusService;
    private final EskaleringService eskaleringService;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final KvpRepository kvpRepository;
    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;
    private final MaalRepository maalRepository;
    private final BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository;

    @Autowired
    public OppfolgingService(
            KafkaProducerService kafkaProducerService,
            YtelserOgAktiviteterService ytelserOgAktiviteterService,
            DkifClient dkifClient,
            KvpService kvpService,
            MetricsService metricsService,
            ArenaOppfolgingService arenaOppfolgingService,
            AuthService authService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            ManuellStatusRepository manuellStatusRepository,
            ManuellStatusService manuellStatusService,
            EskaleringService eskaleringService,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            KvpRepository kvpRepository,
            NyeBrukereFeedRepository nyeBrukereFeedRepository,
            MaalRepository maalRepository,
            BrukerOppslagFlereOppfolgingAktorRepository brukerOppslagFlereOppfolgingAktorRepository
    ) {
        this.kafkaProducerService = kafkaProducerService;
        this.ytelserOgAktiviteterService = ytelserOgAktiviteterService;
        this.dkifClient = dkifClient;
        this.kvpService = kvpService;
        this.metricsService = metricsService;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.authService = authService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.manuellStatusRepository = manuellStatusRepository;
        this.manuellStatusService = manuellStatusService;
        this.eskaleringService = eskaleringService;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
        this.kvpRepository = kvpRepository;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
        this.maalRepository = maalRepository;
        this.brukerOppslagFlereOppfolgingAktorRepository = brukerOppslagFlereOppfolgingAktorRepository;
    }

    @Transactional
    public OppfolgingStatusData hentOppfolgingsStatus(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        sjekkStatusIArenaOgOppdaterOppfolging(fnr);

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
    @Transactional
    public OppfolgingStatusData startOppfolging(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedFnr(fnr);

        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        authService.sjekkTilgangTilEnhet(arenaOppfolgingTilstand.getOppfolgingsenhet());

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);

        if (ArenaUtils.kanSettesUnderOppfolging(arenaOppfolgingTilstand, oppfolging.isUnderOppfolging())) {
            startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        }

        return getOppfolgingStatusData(fnr);
    }

    @SneakyThrows
    @Transactional
    public AvslutningStatusData avsluttOppfolging(Fnr fnr, String veilederId, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedFnr(fnr);

        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        authService.sjekkTilgangTilEnhet(arenaOppfolgingTilstand.getOppfolgingsenhet());

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);

        boolean erIserv = erIserv(arenaOppfolgingTilstand.getFormidlingsgruppe());

        if (kanAvslutteOppfolging(aktorId, oppfolging.isUnderOppfolging(), erIserv)) {
            log.info("Avslutting av oppfølging, tilstand i Arena for aktorid {}: {}", aktorId, arenaOppfolgingTilstand);
            avsluttOppfolgingForBruker(aktorId, veilederId, begrunnelse);
        }

        return getAvslutningStatus(fnr);
    }

    @Transactional
    public boolean avsluttOppfolgingForSystemBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        ArenaOppfolgingTilstand arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr)
                .orElseThrow();

        log.info("Avslutting av oppfølging, tilstand i Arena for aktorid {}: {}", aktorId, arenaOppfolgingTilstand);

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);

        boolean erIserv = erIserv(arenaOppfolgingTilstand.getFormidlingsgruppe());

        if (!kanAvslutteOppfolging(aktorId, oppfolging.isUnderOppfolging(), erIserv)) {
            return false;
        }

        avsluttOppfolgingForBruker(aktorId, SYSTEM_USER_NAME, "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager");
        return true;
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return oppfolgingsPeriodeRepository.fetchAvsluttetEtterDato(timestamp, pageSize);
    }

    @SneakyThrows
    public VeilederTilgang hentVeilederTilgang(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        Optional<VeilarbArenaOppfolging> arenaBruker = arenaOppfolgingService.hentOppfolgingFraVeilarbarena(fnr);
        String oppfolgingsenhet = arenaBruker.map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);
        boolean tilgangTilEnhet = authService.harTilgangTilEnhet(oppfolgingsenhet);
        return new VeilederTilgang().setTilgangTilBrukersKontor(tilgangTilEnhet);
    }

    public List<Oppfolgingsperiode> hentOppfolgingsperioder(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
    }

    public UnderOppfolgingDTO oppfolgingData(Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        return getOppfolgingStatus(fnr)
                .map(oppfolgingsstatus -> {
                    boolean isUnderOppfolging = oppfolgingsstatus.isUnderOppfolging();
                    return new UnderOppfolgingDTO().setUnderOppfolging(isUnderOppfolging).setErManuell(isUnderOppfolging && manuellStatusService.erManuell(oppfolgingsstatus));
                })
                .orElse(new UnderOppfolgingDTO().setUnderOppfolging(false).setErManuell(false));
    }

    public boolean underOppfolgingNiva3(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkTilgangTilPersonMedNiva3(aktorId);

        return ofNullable(oppfolgingsStatusRepository.fetch(aktorId))
                .map(OppfolgingTable::isUnderOppfolging)
                .orElse(false);
    }

    private Optional<OppfolgingTable> getOppfolgingStatus(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);
        return ofNullable(oppfolgingsStatusRepository.fetch(aktorId));
    }

    private OppfolgingStatusData getOppfolgingStatusData(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        Oppfolging oppfolging = hentOppfolging(aktorId)
                .orElse(new Oppfolging().setAktorId(aktorId.get()).setUnderOppfolging(false));

        boolean erManuell = ofNullable(oppfolging.getGjeldendeManuellStatus())
                .map(ManuellStatus::isManuell)
                .orElse(false);

        DkifKontaktinfo dkifKontaktinfo = dkifClient.hentKontaktInfo(fnr);

        if (oppfolging.isUnderOppfolging() && !erManuell && dkifKontaktinfo.isReservert()) {
            manuellStatusRepository.create(
                    new ManuellStatus()
                            .setAktorId(aktorId.get())
                            .setManuell(true)
                            .setDato(ZonedDateTime.now())
                            .setBegrunnelse("Brukeren er reservert i Kontakt- og reservasjonsregisteret")
                            .setOpprettetAv(SYSTEM)
            );
        }

        // TODO: Burde kanskje heller feile istedenfor å bruke Optional
        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = arenaOppfolgingService.hentOppfolgingTilstand(fnr);

        boolean kanSettesUnderOppfolging = !oppfolging.isUnderOppfolging() && maybeArenaOppfolging
                .map(s -> kanSettesUnderOppfolging(s.getFormidlingsgruppe(), s.getServicegruppe()))
                .orElse(false);

        long kvpId = kvpRepository.gjeldendeKvp(aktorId);
        boolean harSkrivetilgangTilBruker = !kvpService.erUnderKvp(kvpId) || authService.harTilgangTilEnhet(kvpRepository.fetch(kvpId).getEnhet());

        Boolean erInaktivIArena = maybeArenaOppfolging.map(ao -> erIserv(ao.getFormidlingsgruppe())).orElse(null);

        Optional<Boolean> maybeKanEnkeltReaktiveres = maybeArenaOppfolging.map(ArenaOppfolgingTilstand::getKanEnkeltReaktiveres);

        Boolean kanReaktiveres = maybeKanEnkeltReaktiveres
                .map(kr -> oppfolging.isUnderOppfolging() && kr)
                .orElse(null);

        Boolean erSykmeldtMedArbeidsgiver = maybeArenaOppfolging
                .map(ao -> ArenaUtils.erIARBSUtenOppfolging(ao.getFormidlingsgruppe(), ao.getServicegruppe()))
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
                .setReservasjonKRR(dkifKontaktinfo.isReservert())
                .setManuell(erManuell || dkifKontaktinfo.isReservert())
                .setKanStarteOppfolging(kanSettesUnderOppfolging)
                .setGjeldendeEskaleringsvarsel(oppfolging.getGjeldendeEskaleringsvarsel())
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
                .setKanVarsles(!erManuell && dkifKontaktinfo.isKanVarsles());
    }

    private AvslutningStatusData getAvslutningStatus(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);

        Optional<ArenaOppfolgingTilstand> maybeArenaOppfolging = arenaOppfolgingService.hentOppfolgingTilstand(fnr);

        boolean erIserv = maybeArenaOppfolging.map(ao -> erIserv(ao.getFormidlingsgruppe())).orElse(false);

        boolean kanAvslutte = kanAvslutteOppfolging(aktorId, oppfolging.isUnderOppfolging(), erIserv);

        boolean erUnderOppfolging = maybeArenaOppfolging
                .map(status -> erUnderOppfolging(status.getFormidlingsgruppe(), status.getServicegruppe()))
                .orElse(false);

        LocalDate inaktiveringsDato = maybeArenaOppfolging
                .map(ArenaOppfolgingTilstand::getInaktiveringsdato)
                .orElse(null);

        return AvslutningStatusData.builder()
                .kanAvslutte(kanAvslutte)
                .underOppfolging(erUnderOppfolging)
                .harYtelser(ytelserOgAktiviteterService.harPagaendeYtelse(fnr))
                .underKvp(kvpService.erUnderKvp(aktorId))
                .inaktiveringsDato(inaktiveringsDato)
                .build();
    }

    public Oppfolgingsperiode hentPeriode(String uuid) {
        return oppfolgingsPeriodeRepository.hentOppfolgingsperiode(uuid);
    }

    @SneakyThrows
    public Optional<Oppfolging> hentOppfolging(AktorId aktorId) {
        OppfolgingTable t = oppfolgingsStatusRepository.fetch(aktorId);

        if (t == null) {
            return Optional.empty();
        }

        Oppfolging o = new Oppfolging()
                .setAktorId(t.getAktorId())
                .setVeilederId(t.getVeilederId())
                .setUnderOppfolging(t.isUnderOppfolging());

        Kvp kvp = null;
        if (t.getGjeldendeKvpId() != 0) {
            kvp = kvpRepository.fetch(t.getGjeldendeKvpId());
            if (authService.harTilgangTilEnhet(kvp.getEnhet())) {
                o.setGjeldendeKvp(kvp);
            }
        }

        // Gjeldende eskaleringsvarsel inkluderes i resultatet kun hvis den innloggede veilederen har tilgang til brukers enhet.
        if (t.getGjeldendeEskaleringsvarselId() != 0) {
            EskaleringsvarselData varsel = eskaleringsvarselRepository.fetch(t.getGjeldendeEskaleringsvarselId());
            if (sjekkTilgangGittKvp(authService, kvp, varsel::getOpprettetDato)) {
                o.setGjeldendeEskaleringsvarsel(varsel);
            }
        }

        if (t.getGjeldendeMaalId() != 0) {
            o.setGjeldendeMal(maalRepository.fetch(t.getGjeldendeMaalId()));
        }

        if (t.getGjeldendeManuellStatusId() != 0) {
            o.setGjeldendeManuellStatus(manuellStatusRepository.fetch(t.getGjeldendeManuellStatusId()));
        }

        List<Kvp> kvpPerioder = kvpRepository.hentKvpHistorikk(aktorId);
        o.setOppfolgingsperioder(populerKvpPerioder(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AktorId.of(t.getAktorId())), kvpPerioder));

        return Optional.of(o);
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(AktorId aktorId) {
        startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.get())
                        .build()
        );
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker oppfolgingsbruker) {
        AktorId aktorId = AktorId.of(oppfolgingsbruker.getAktoerId());

        OppfolgingTable eksisterendeOppfolging = oppfolgingsStatusRepository.fetch(aktorId);

        if (eksisterendeOppfolging != null && eksisterendeOppfolging.isUnderOppfolging()) {
            return;
        }

        if (eksisterendeOppfolging == null) {
            // Siden det blir gjort mange kall samtidig til flere noder kan det oppstå en race condition
            // hvor oppfølging har blitt insertet av en annen node etter at den har sjekket at oppfølging
            // ikke ligger i databasen.
            try {
                oppfolgingsStatusRepository.opprettOppfolging(aktorId);
            } catch (DuplicateKeyException e) {
                log.warn("Race condition oppstod under oppretting av ny oppfølging for bruker: " + aktorId);
                return;
            }
        }

        oppfolgingsPeriodeRepository.start(aktorId);
        nyeBrukereFeedRepository.leggTil(oppfolgingsbruker);

        List<Oppfolgingsperiode> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        Oppfolgingsperiode sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

        kafkaProducerService.publiserSisteOppfolgingsperiode(DtoMappers.tilSisteOppfolgingsperiodeV1(sistePeriode));
        kafkaProducerService.publiserOppfolgingStartet(aktorId, sistePeriode.getStartDato());
    }

    private List<Oppfolgingsperiode> populerKvpPerioder(List<Oppfolgingsperiode> oppfolgingsPerioder, List<Kvp> kvpPerioder) {
        return oppfolgingsPerioder.stream()
                .map(periode -> periode.toBuilder().kvpPerioder(
                        kvpPerioder.stream()
                                .filter(kvp -> authService.harTilgangTilEnhetMedSperre(kvp.getEnhet()) )
                                .filter(kvp -> erKvpIPeriode(kvp, periode))
                                .collect(toList())
                ).build())
                .collect(toList());
    }

    private boolean erKvpIPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return kvpEtterStartenAvPeriode(kvp, periode)
                && kvpForSluttenAvPeriode(kvp, periode);
    }

    private boolean kvpEtterStartenAvPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return !periode.getStartDato().isAfter(kvp.getOpprettetDato());
    }

    private boolean kvpForSluttenAvPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return periode.getSluttDato() == null || !periode.getSluttDato().isBefore(kvp.getOpprettetDato());
    }

    private void sjekkStatusIArenaOgOppdaterOppfolging(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        Optional<ArenaOppfolgingTilstand> arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstand(fnr);

        arenaOppfolgingTilstand.ifPresent(oppfolgingTilstand -> {
            Optional<OppfolgingTable> maybeOppfolging = ofNullable(oppfolgingsStatusRepository.fetch(aktorId));

            boolean erBrukerUnderOppfolging = maybeOppfolging.map(OppfolgingTable::isUnderOppfolging).orElse(false);
            boolean erUnderOppfolgingIArena = erUnderOppfolging(oppfolgingTilstand.getFormidlingsgruppe(), oppfolgingTilstand.getServicegruppe());

            if (!erBrukerUnderOppfolging && erUnderOppfolgingIArena) {
                startOppfolgingHvisIkkeAlleredeStartet(aktorId);
            } else {
                boolean erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver(oppfolgingTilstand);
                boolean erInaktivIArena = erInaktivIArena(oppfolgingTilstand);
                boolean sjekkIArenaOmBrukerSkalAvsluttes = erBrukerUnderOppfolging && erInaktivIArena;

                log.info("Statuser for reaktivering og inaktivering basert på {}: "
                                + "Aktiv Oppfølgingsperiode={} "
                                + "erSykmeldtMedArbeidsgiver={} "
                                + "inaktivIArena={} "
                                + "aktorId={} "
                                + "Tilstand i Arena: {}",
                        oppfolgingTilstand.isDirekteFraArena() ? "Arena" : "Veilarbarena",
                        erBrukerUnderOppfolging,
                        erSykmeldtMedArbeidsgiver,
                        erInaktivIArena,
                        aktorId,
                        arenaOppfolgingTilstand);

                if (sjekkIArenaOmBrukerSkalAvsluttes) {
                    sjekkOgOppdaterBrukerDirekteFraArena(fnr, oppfolgingTilstand, maybeOppfolging.get());
                }
            }
        });
    }

    private void sjekkOgOppdaterBrukerDirekteFraArena(Fnr fnr, ArenaOppfolgingTilstand arenaOppfolgingTilstand, OppfolgingTable oppfolging) {
        Optional<ArenaOppfolgingTilstand> maybeTilstandDirekteFraArena = arenaOppfolgingTilstand.isDirekteFraArena()
                ? of(arenaOppfolgingTilstand)
                : arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(fnr);

        maybeTilstandDirekteFraArena.ifPresent(tilstandDirekteFraArena -> {
            boolean erInaktivIArena = erInaktivIArena(tilstandDirekteFraArena);
            boolean kanEnkeltReaktiveres = TRUE.equals(tilstandDirekteFraArena.getKanEnkeltReaktiveres());
            boolean skalAvsluttes = oppfolging.isUnderOppfolging() && erInaktivIArena && !kanEnkeltReaktiveres;

            log.info("Mulig avslutting av oppfølging "
                            + "erUnderOppfolging={} "
                            + "kanEnkeltReaktiveres={} "
                            + "inaktivIArena={} "
                            + "skalAvsluttes={} "
                            + "aktorId={} "
                            + "Tilstand i Arena: {}",
                    oppfolging.isUnderOppfolging(),
                    kanEnkeltReaktiveres,
                    erInaktivIArena,
                    skalAvsluttes,
                    oppfolging.getAktorId(),
                    arenaOppfolgingTilstand);

            if (skalAvsluttes) {
                AktorId aktorId = authService.getAktorIdOrThrow(fnr);
                boolean kanAvslutte = kanAvslutteOppfolging(aktorId, oppfolging.isUnderOppfolging(), erIserv(tilstandDirekteFraArena.getFormidlingsgruppe()));
                inaktiverBruker(aktorId, kanAvslutte);
            }
        });
    }

    private void inaktiverBruker(AktorId aktorId, boolean kanAvslutteOppfolging) {
        log.info("Avslutter oppfølgingsperiode for bruker");

        if (kanAvslutteOppfolging) {
            avsluttOppfolgingForBruker(aktorId, null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
        } else {
            log.info("Avslutting av oppfølging ikke tillatt for aktorid {}", aktorId);
        }

        metricsService.raporterAutomatiskAvslutningAvOppfolging(!kanAvslutteOppfolging);
    }

    public boolean kanAvslutteOppfolging(AktorId aktorId, boolean erUnderOppfolging, boolean erIservIArena) {
        boolean ikkeUnderKvp = !kvpService.erUnderKvp(aktorId);

        log.info("Kan oppfolging avsluttes for aktorid {}?, oppfolging.isUnderOppfolging(): {}, erIservIArena(): {}, !erUnderKvp(): {}",
                aktorId, erUnderOppfolging, erIservIArena, ikkeUnderKvp);

        return erUnderOppfolging
                && erIservIArena
                && ikkeUnderKvp;
    }

    private void avsluttOppfolgingForBruker(AktorId aktorId, String veilederId, String begrunnelse) {
        String brukerIdent = authService.getInnloggetBrukerIdent();
        eskaleringService.stoppEskaleringForAvsluttOppfolging(aktorId, brukerIdent, begrunnelse);

        oppfolgingsPeriodeRepository.avslutt(aktorId, veilederId, begrunnelse);

        List<Oppfolgingsperiode> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        Oppfolgingsperiode sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

        kafkaProducerService.publiserSisteOppfolgingsperiode(DtoMappers.tilSisteOppfolgingsperiodeV1(sistePeriode));
        kafkaProducerService.publiserOppfolgingAvsluttet(aktorId);
    }

}
