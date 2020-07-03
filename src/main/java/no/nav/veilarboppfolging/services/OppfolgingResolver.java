package no.nav.veilarboppfolging.services;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.Event;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClient;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.controller.domain.DkifResponse;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.domain.ManuellStatus;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingRepository;
import no.nav.veilarboppfolging.utils.ArenaUtils;
import no.nav.veilarboppfolging.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static no.nav.veilarboppfolging.domain.arena.AktivitetStatus.AVBRUTT;
import static no.nav.veilarboppfolging.domain.arena.AktivitetStatus.FULLFORT;
import static no.nav.veilarboppfolging.utils.ArenaUtils.*;


@Slf4j
public class OppfolgingResolver {

    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private String fnr;
    private OppfolgingResolverDependencies deps;

    private String aktorId;
    private Oppfolging oppfolging;
    private Optional<Either<VeilarbArenaOppfolging, ArenaOppfolging>> arenaOppfolgingTilstand;
    private DkifResponse dkifResponse;
    private YtelseskontraktResponse ytelser;
    private List<ArenaAktivitetDTO> arenaAktiviteter;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
    private Boolean erSykmeldtMedArbeidsgiver;
    private final boolean brukArenaDirekte;

    private OppfolgingResolver(String fnr, OppfolgingResolverDependencies deps, boolean brukArenaDirekte) {
        this.brukArenaDirekte = brukArenaDirekte;

        this.fnr = fnr;
        this.deps = deps;
        this.arenaOppfolgingTilstand = Optional.empty();

        this.aktorId = deps.authService.getAktorIdOrThrow(fnr);
        this.oppfolging = hentOppfolging();

        avsluttKvpVedEnhetBytte();
    }

    public static OppfolgingResolver lagOppfolgingResolver(String fnr, OppfolgingResolverDependencies deps, boolean brukArenaDirekte) {
        return new OppfolgingResolver(fnr, deps, brukArenaDirekte);
    }

    public static OppfolgingResolver lagOppfolgingResolver(String fnr, OppfolgingResolverDependencies deps) {
        return lagOppfolgingResolver(fnr, deps, false);
    }

    private Optional<ArenaOppfolging> oppfolgingDirekteFraArena() {
        return arenaOppfolgingTilstand.flatMap(Either::toJavaOptional);
    }

    private Optional<ArenaOppfolgingTilstand> arenaOppfolgingTilstand() {
        return arenaOppfolgingTilstand.map(this::arenaOppfolgingTilstand);
    }

    private ArenaOppfolgingTilstand arenaOppfolgingTilstand(Either<VeilarbArenaOppfolging, ArenaOppfolging> entenEller) {
        if (entenEller.isRight()) {
            return ArenaOppfolgingTilstand.fraArenaOppfolging(entenEller.get());
        } else {
            return ArenaOppfolgingTilstand.fraArenaBruker(entenEller.getLeft());
        }
    }

    void reloadOppfolging() {
        oppfolging = hentOppfolging();
    }

    void sjekkStatusIArenaOgOppdaterOppfolging() {
        hentOppfolgingstatusFraArena();
        arenaOppfolgingTilstand().ifPresent(arenaBruker -> {
            if (!oppfolging.isUnderOppfolging()) {
                sjekkOgStartOppfolging();
            }
            sjekkOgOppdaterBruker(arenaBruker);

        });
    }

    private void sjekkOgStartOppfolging() {
        arenaOppfolgingTilstand().ifPresent(arenaOppfolging -> {
            if (erUnderOppfolging(arenaOppfolging.getFormidlingsgruppe(), arenaOppfolging.getServicegruppe())) {
                deps.getOppfolgingRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
                reloadOppfolging();
            }
        });
    }

    private void sjekkOgOppdaterBruker(ArenaOppfolgingTilstand arenaOppfolgingTilstand) {

        oppdatertErSykmeldtMedArbeidsgiver(arenaOppfolgingTilstand);
        oppdaterInaktivIArena(arenaOppfolgingTilstand);

        boolean sjekkIArenaOmBrukerSkalAvsluttes = oppfolging.isUnderOppfolging() && inaktivIArena;

        logStatusForReaktiveringOgInaktivering();

        if (sjekkIArenaOmBrukerSkalAvsluttes) {
            sjekkOgOppdaterBrukerDirekteFraArena();
        }
    }

    private void logStatusForReaktiveringOgInaktivering() {
        log.info("Statuser for reaktivering og inaktivering basert på {}: "
                        + "Aktiv Oppfølgingsperiode={} "
                        + "kanEnkeltReaktiveres={} "
                        + "erSykmeldtMedArbeidsgiver={} "
                        + "inaktivIArena={} "
                        + "aktorId={} "
                        + "Tilstand i Arena: {}",
                tilstandFra(),
                oppfolging.isUnderOppfolging(),
                kanReaktiveres,
                erSykmeldtMedArbeidsgiver,
                inaktivIArena,
                aktorId,
                arenaOppfolgingTilstandToString());
    }

    private String tilstandFra() {
        return arenaOppfolgingTilstand.map(tilstand ->
                tilstand.fold(fraArena -> "veilarbarena",
                        fraVeilarbarena -> "Arena"))
                .orElse(null);
    }

    private String arenaOppfolgingTilstandToString() {
        return arenaOppfolgingTilstand
                .map(tilstand ->
                        tilstand.fold(
                                VeilarbArenaOppfolging::toString,
                                ArenaOppfolging::toString))
                .orElse(null);
    }

    private void oppdatertErSykmeldtMedArbeidsgiver(ArenaOppfolgingTilstand arenaOppfolgingTilstand) {

        erSykmeldtMedArbeidsgiver = ArenaUtils.erIARBSUtenOppfolging(
                arenaOppfolgingTilstand.getFormidlingsgruppe(),
                arenaOppfolgingTilstand.getServicegruppe()
        );
    }

    private void oppdaterInaktivIArena(ArenaOppfolgingTilstand arenaOppfolgingTilstand) {
        inaktivIArena = erIserv(arenaOppfolgingTilstand.getFormidlingsgruppe());
    }


    private void sjekkOgOppdaterBrukerDirekteFraArena() {
        hentOppfolgingstatusDirekteFraArena();

        arenaOppfolgingTilstand().ifPresent(this::oppdatertErSykmeldtMedArbeidsgiver);
        arenaOppfolgingTilstand().ifPresent(this::oppdaterInaktivIArena);

        oppfolgingDirekteFraArena().ifPresent(arenaOppfolging -> {

            boolean kanEnkeltReaktiveres = ArenaUtils.kanEnkeltReaktiveres(arenaOppfolging);
            kanReaktiveres = oppfolging.isUnderOppfolging() && kanEnkeltReaktiveres;
            boolean skalAvsluttes = oppfolging.isUnderOppfolging() && inaktivIArena && !kanEnkeltReaktiveres;

            logStatusForReaktiveringOgInaktivering();

            if (skalAvsluttes) {
                inaktiverBruker();
            }
        });
    }

    private void inaktiverBruker() {
        log.info("Avslutter oppfølgingsperiode for bruker");
        avsluttOppfolging(null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
        reloadOppfolging();
        deps.getMetricsService().raporterAutomatiskAvslutningAvOppfolging(!oppfolging.isUnderOppfolging());
    }

    List<MalData> getMalList() {
        return deps.getOppfolgingRepository().hentMalList(aktorId);
    }

    MalData oppdaterMal(String mal, String endretAvVeileder) {
        MalData malData = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(StringUtils.of(endretAvVeileder).orElse(aktorId))
                .setDato(new Timestamp(currentTimeMillis()));
        deps.getOppfolgingRepository().opprettMal(malData);
        return hentOppfolging().getGjeldendeMal();
    }

    Oppfolging getOppfolging() {
        return oppfolging;
    }

    String getAktorId() {
        return aktorId;
    }

    DkifResponse reservertIKrr() {
        if (dkifResponse == null) {
            sjekkReservasjonIKrrOgOppdaterOppfolging();
        }
        return dkifResponse;
    }

    boolean manuell() {
        return ofNullable(oppfolging.getGjeldendeManuellStatus())
                .map(ManuellStatus::isManuell)
                .orElse(false);
    }

    boolean getKanSettesUnderOppfolging() {
        if (oppfolging.isUnderOppfolging()) {
            return false;
        }
        hentOppfolgingstatusFraArena();
        return arenaOppfolgingTilstand().map(status ->
                kanSettesUnderOppfolging(status.getFormidlingsgruppe(),
                        status.getServicegruppe()))
                .orElse(false);
    }

    void startOppfolging() {
        deps.getOppfolgingRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging = hentOppfolging();
    }

    boolean erUnderOppfolgingIArena() {
        hentOppfolgingstatusFraArena();
        return arenaOppfolgingTilstand().map(status ->
                erUnderOppfolging(status.getFormidlingsgruppe(), status.getServicegruppe()))
                .orElse(false);
    }

    boolean harPagaendeYtelse() {
        if (ytelser == null) {
            this.ytelser = deps.getYtelseskontraktClient().hentYtelseskontraktListe(fnr);
        }

        return ytelser.getYtelser()
                .stream()
                .anyMatch(ytelseskontrakt -> AKTIV_YTELSE_STATUS.equals(ytelseskontrakt.getStatus()));
    }

    boolean harAktiveTiltak() {
        if (arenaAktiviteter == null) {
            hentArenaAktiviteter();
        }
        return arenaAktiviteter
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    boolean erUnderKvp() {
        return deps.getKvpRepository().gjeldendeKvp(getAktorId()) != 0L;
    }

    boolean harSkrivetilgangTilBruker() {
        long kvpId = deps.getKvpRepository().gjeldendeKvp(getAktorId());
        return kvpId == 0L || tilgangTilEnhet(kvpId);
    }

    @SneakyThrows
    private boolean tilgangTilEnhet(long kvpId) {
        String enhet = deps.getKvpRepository().fetch(kvpId).getEnhet();
        return deps.getAuthService().harTilgangTilEnhet(enhet);
    }

    boolean kanAvslutteOppfolging() {
        log.info("Kan oppfolging avsluttes for aktorid {}?, oppfolging.isUnderOppfolging(): {}, erIservIArena(): {}, !erUnderKvp(): {}",
                aktorId, oppfolging.isUnderOppfolging(), erIservIArena(), !erUnderKvp());
        return oppfolging.isUnderOppfolging()
                && erIservIArena()
                && !erUnderKvp();
    }

    private boolean erIservIArena() {
        hentOppfolgingstatusFraArena();
        return arenaOppfolgingTilstand().map(status -> erIserv(status.getFormidlingsgruppe())).orElse(false);
    }

    Date getInaktiveringsDato() {
        hentOppfolgingstatusFraArena();

        return arenaOppfolgingTilstand().map(this::getInaktiveringsDato).orElse(null);
    }

    String getServicegruppe() {
        return arenaOppfolgingTilstand().map(ArenaOppfolgingTilstand::getServicegruppe).orElse(null);
    }

    String getFormidlingsgruppe() {
        return arenaOppfolgingTilstand().map(ArenaOppfolgingTilstand::getFormidlingsgruppe).orElse(null);
    }

    String getRettighetsgruppe() {
        return arenaOppfolgingTilstand().map(ArenaOppfolgingTilstand::getRettighetsgruppe).orElse(null);
    }

    private Date getInaktiveringsDato(ArenaOppfolgingTilstand status) {
        return Optional.ofNullable(status.getInaktiveringsdato()).isPresent()
                ? Date.from(status.getInaktiveringsdato().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                : null;
    }

    String getOppfolgingsEnhet() {
        hentOppfolgingstatusFraArena();
        return arenaOppfolgingTilstand().map(status -> status.getOppfolgingsenhet()).orElse(null);
    }

    Boolean getInaktivIArena() {
        return inaktivIArena;
    }

    Boolean getKanReaktiveres() {
        return kanReaktiveres;
    }

    Boolean getErSykmeldtMedArbeidsgiver() {
        return erSykmeldtMedArbeidsgiver;
    }

    boolean avsluttOppfolging(String veileder, String begrunnelse) {
        boolean oppfolgingKanAvsluttes = kanAvslutteOppfolging();

        hentOppfolgingstatusFraArena();

        arenaOppfolgingTilstand().ifPresent(arenaStatus ->
                log.info("Avslutting av oppfølging, tilstand i Arena for aktorid {}: {}", aktorId, arenaStatus));


        if (!oppfolgingKanAvsluttes) {
            log.info("Avslutting av oppfølging ikke tillatt for aktorid {}", aktorId);
            return false;
        }

        if (Optional.ofNullable(oppfolging.getGjeldendeEskaleringsvarsel()).isPresent()) {
            stoppEskalering("Eskalering avsluttet fordi oppfølging ble avsluttet");
        }

        avsluttOppfolgingOgSendPaKafka(veileder, begrunnelse);
        return true;
    }

    @SneakyThrows
    @Transactional
    void avsluttOppfolgingOgSendPaKafka(String veileder, String begrunnelse) {
        deps.getOppfolgingRepository().avsluttOppfolging(aktorId, veileder, begrunnelse);
        deps.getAvsluttOppfolgingProducer().avsluttOppfolgingEvent(aktorId, LocalDateTime.now());
    }

    private Oppfolging hentOppfolging() {
        return deps.getOppfolgingRepository().hentOppfolging(aktorId)
                .orElseGet(() -> new Oppfolging().setAktorId(aktorId).setUnderOppfolging(false));
    }

    void startEskalering(String begrunnelse, long tilhorendeDialogId) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        deps.getTransactor().executeWithoutResult((status) -> {
            deps.getOppfolgingRepository().startEskalering(aktorId, veilederId, begrunnelse, tilhorendeDialogId);
            deps.getVarseloppgaveClient().sendEskaleringsvarsel(aktorId, tilhorendeDialogId);
        });
    }

    void stoppEskalering(String begrunnelse) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        deps.getOppfolgingRepository().stoppEskalering(aktorId, veilederId, begrunnelse);
    }

    @SneakyThrows
    private void hentOppfolgingstatusFraArena() {
        if (arenaOppfolgingTilstand.isPresent()) {
            return;
        }

        if (brukArenaDirekte || deps.getUnleashService().isEnabled("veilarboppfolging.oppfolgingresolver.bruk_arena_direkte")) {
            hentOppfolgingstatusDirekteFraArena();
        } else {
            hentOppfolgingstatusFraVeilarbArena();

            boolean harTilstand = arenaOppfolgingTilstand.isPresent();
            boolean erUnderOppfolgingIVeilarbarena = arenaOppfolgingTilstand().filter(oppfolgingTilstand ->
                    ArenaUtils.erUnderOppfolging(oppfolgingTilstand.getFormidlingsgruppe(), oppfolgingTilstand.getServicegruppe())
            ).isPresent();

            boolean harIkkeDataIVeilarbarena = !harTilstand;
            boolean erIkkeUnderOppfolgingIVeilarbarena = !erUnderOppfolgingIVeilarbarena;

            // Fallbackløsning for å hente direkte fra Arena dersom data fra veilarbarena ikke stemmer overens
            // med oppfølgingsflagg:

            if ((harIkkeDataIVeilarbarena || erIkkeUnderOppfolgingIVeilarbarena) && oppfolging.isUnderOppfolging()) {
                // Dette kan forekomme direkte etter registrering, før data har blitt synkronisert fra Arena til veilarbarena.
                // Enten kan det mangle data i veilarbarena, eller så kan det være gammel data som ikke er fra den nye registreringen
                oppfolgingDirekteFraArenaMetrikk();
                hentOppfolgingstatusDirekteFraArena();
            } else if (!oppfolging.isUnderOppfolging() && erUnderOppfolgingIVeilarbarena) {
                // Dette kan forekomme etter at bruker er tatt ut av oppfølging, men før før data har blitt synkronisert fra Arena til veilarbarena.
                oppfolgingDirekteFraArenaMetrikk();
                hentOppfolgingstatusDirekteFraArena();
            }
        }
    }


    @SneakyThrows
    private void hentOppfolgingstatusDirekteFraArena() {
        if (oppfolgingDirekteFraArena().isPresent()) {
            return;
        }

        arenaOppfolgingTilstand = Try.of(() -> deps.getVeilarbarenaClient().getArenaOppfolgingsstatus(fnr))
                .onFailure(e -> log.warn("Feil fra Arena for aktørId: {}", aktorId, e))
                .toJavaOptional().map(Either::right);
    }

    private void oppfolgingDirekteFraArenaMetrikk() {
        Event event = new Event("veilarboppfolging.oppfolgingdirektefraarena")
                .addTagToReport("formidlingsgruppe", arenaOppfolgingTilstand().map(ArenaOppfolgingTilstand::getFormidlingsgruppe).orElse("INGEN_VERDI"))
                .addTagToReport("servicegruppe", arenaOppfolgingTilstand().map(ArenaOppfolgingTilstand::getServicegruppe).orElse("INGEN_VERDI"))
                .addTagToReport("underOppfolging", Boolean.toString(oppfolging.isUnderOppfolging()))
                .addTagToReport("manueltRegistrert", (oppfolging.getGjeldendeManuellStatus() != null && oppfolging.getGjeldendeManuellStatus().isManuell()) ? "ja" : "nei");

        deps.getMetricsService().report(event);
    }


    private void hentOppfolgingstatusFraVeilarbArena() {
        if (!arenaOppfolgingTilstand.isPresent()) {
            Optional<VeilarbArenaOppfolging> veilarbArenaOppfolging =
                    deps.getVeilarbarenaClient().hentOppfolgingsbruker(fnr);

            arenaOppfolgingTilstand = veilarbArenaOppfolging.map(Either::left);
        }
    }

    private void sjekkReservasjonIKrrOgOppdaterOppfolging() {
        if (oppfolging.isUnderOppfolging()) {
            this.dkifResponse = sjekkKrr();
            if (!manuell() && dkifResponse.isKrr()) {
                deps.getOppfolgingRepository().opprettManuellStatus(
                        new ManuellStatus()
                                .setAktorId(oppfolging.getAktorId())
                                .setManuell(true)
                                .setDato(new Timestamp(currentTimeMillis()))
                                .setBegrunnelse("Reservert og under oppfølging")
                                .setOpprettetAv(SYSTEM)
                );
            }
        } else {
            this.dkifResponse = new DkifResponse().setKrr(false).setKanVarsles(true);
        }
    }

    @SneakyThrows
    private DkifResponse sjekkKrr() {
        try {
            DkifKontaktinfo kontaktinfo = deps.getDkifClient().hentKontaktInfo(fnr);

            boolean kanVarsles = kontaktinfo.isKanVarsles();
            boolean krr = kontaktinfo.isReservert();

            log.info("Dkif-response: {}: kanVarsles: {} krr: {}", aktorId, kanVarsles, krr);

            return new DkifResponse().setKrr(krr).setKanVarsles(kanVarsles);
        } catch(Exception e) {
            log.warn("Feil fra Dkif for aktørId: {}", aktorId, e);
            return new DkifResponse().setKrr(true).setKanVarsles(false);
        }
    }

    private void hentArenaAktiviteter() {
        this.arenaAktiviteter = deps.getVeilarbaktivitetClient().hentArenaAktiviteter(fnr);
    }

    private void avsluttKvpVedEnhetBytte() {
        Kvp gjeldendeKvp = deps.getKvpService().gjeldendeKvp(aktorId);
        if (gjeldendeKvp == null) {
            return;
        }

        hentOppfolgingstatusFraArena();
        arenaOppfolgingTilstand().ifPresent(status -> {
            if (brukerHarByttetKontor(status, gjeldendeKvp)) {
                deps.getKvpService().stopKvpUtenEnhetSjekk(aktorId, "KVP avsluttet automatisk pga. endret Nav-enhet", SYSTEM);
                deps.getMetricsService().stopKvpDueToChangedUnit();
                reloadOppfolging();
            }
        });
    }

    private boolean brukerHarByttetKontor(ArenaOppfolgingTilstand statusIArena, Kvp kvp) {
        return !statusIArena.getOppfolgingsenhet().equals(kvp.getEnhet());
    }

    @Component
    @Getter
    public static class OppfolgingResolverDependencies {

        @Autowired
        private MetricsService metricsService;

        @Autowired
        private AuthService authService;

        @Autowired
        private VeilarbarenaClient veilarbarenaClient;

        @Autowired
        private TransactionTemplate transactor;

        @Autowired
        private OppfolgingRepository oppfolgingRepository;

        @Autowired
        private DkifClient dkifClient;

        @Autowired
        private YtelseskontraktClient ytelseskontraktClient;

        @Autowired
        private VeilarbaktivitetClient veilarbaktivitetClient;

        @Autowired
        private KvpRepository kvpRepository;

        @Autowired
        private KvpService kvpService;

        @Autowired
        private VarseloppgaveClient varseloppgaveClient;

        @Autowired
        private UnleashService unleashService;

        @Autowired
        private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    }


}

/**
 * Felles struktur for data som kan hentes både fra Arena og veilarbarena
 */
@Value
class ArenaOppfolgingTilstand {
    String formidlingsgruppe;
    String servicegruppe;
    String rettighetsgruppe;
    String oppfolgingsenhet;
    LocalDate inaktiveringsdato;

    static ArenaOppfolgingTilstand fraArenaOppfolging(ArenaOppfolging arenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                arenaOppfolging.getFormidlingsgruppe(),
                arenaOppfolging.getServicegruppe(),
                arenaOppfolging.getRettighetsgruppe(),
                arenaOppfolging.getOppfolgingsenhet(),
                arenaOppfolging.getInaktiveringsdato());
    }

    static ArenaOppfolgingTilstand fraArenaBruker(VeilarbArenaOppfolging veilarbArenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                veilarbArenaOppfolging.getFormidlingsgruppekode(),
                veilarbArenaOppfolging.getKvalifiseringsgruppekode(),
                veilarbArenaOppfolging.getRettighetsgruppekode(),
                veilarbArenaOppfolging.getNav_kontor(),
                Optional.ofNullable(veilarbArenaOppfolging.getIserv_fra_dato()).map(ZonedDateTime::toLocalDate).orElse(null)
        );
    }

}
