package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.fo.veilarboppfolging.utils.StringUtils;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.jdbc.Transactor;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static no.nav.fo.veilarboppfolging.domain.arena.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarboppfolging.domain.arena.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.*;


@Slf4j
public class OppfolgingResolver {

    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private String fnr;
    private OppfolgingResolverDependencies deps;

    private String aktorId;
    private Oppfolging oppfolging;
    private Optional<Either<VeilarbArenaOppfolging, ArenaOppfolging>> arenaOppfolgingTilstand;
    private Boolean reservertIKrr;
    private WSHentYtelseskontraktListeResponse ytelser;
    private List<ArenaAktivitetDTO> arenaAktiviteter;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
    private Boolean erSykmeldtMedArbeidsgiver;
    private final boolean brukArenaDirekte;

    OppfolgingResolver(String fnr, OppfolgingResolverDependencies deps) {
        this(fnr, deps, false);
    }

    OppfolgingResolver(String fnr, OppfolgingResolverDependencies deps, boolean brukArenaDirekte) {
        this.brukArenaDirekte = brukArenaDirekte;
        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> this.deps.getAktorService().getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørid")));

        deps.getPepClient().sjekkLesetilgangTilBruker(bruker);

        this.fnr = fnr;
        this.deps = deps;
        this.arenaOppfolgingTilstand = Optional.empty();

        this.aktorId = bruker.getAktoerId();
        this.oppfolging = hentOppfolging();

        avsluttKvpVedEnhetBytte();
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
        hentOppfolgingstatusDirekteFraArena();
        oppfolgingDirekteFraArena().ifPresent(arenaOppfolging -> {
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
        MetricsFactory.createEvent("oppfolging.automatisk.avslutning").addFieldToReport("success", !oppfolging.isUnderOppfolging()).report();
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

    boolean reservertIKrr() {
        if (reservertIKrr == null) {
            sjekkReservasjonIKrrOgOppdaterOppfolging();
        }
        return reservertIKrr;
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
            hentYtelseskontrakt();
        }
        return ytelser.getYtelseskontraktListe()
                .stream()
                .map(WSYtelseskontrakt::getStatus)
                .anyMatch(AKTIV_YTELSE_STATUS::equals);
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
        return deps.getPepClient().harTilgangTilEnhet(enhet);
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
        deps.getAvsluttOppfolgingProducer().avsluttOppfolgingEvent(aktorId, new Date());
    }

    private Oppfolging hentOppfolging() {
        return deps.getOppfolgingRepository().hentOppfolging(aktorId)
                .orElseGet(() -> new Oppfolging().setAktorId(aktorId).setUnderOppfolging(false));
    }

    void startEskalering(String begrunnelse, long tilhorendeDialogId) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        deps.getTransactor().inTransaction(() -> {
            deps.getOppfolgingRepository().startEskalering(aktorId, veilederId, begrunnelse, tilhorendeDialogId);
            deps.getEskaleringsvarselService().sendEskaleringsvarsel(aktorId, tilhorendeDialogId);
        });
    }

    void stoppEskalering(String begrunnelse) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        deps.getOppfolgingRepository().stoppEskalering(aktorId, veilederId, begrunnelse);
    }

    boolean harAktivEskalering() {
        return oppfolging.getGjeldendeEskaleringsvarsel() != null;
    }

    @SneakyThrows
    private void hentOppfolgingstatusFraArena() {
        if (!arenaOppfolgingTilstand.isPresent()) {
            if (brukArenaDirekte || deps.getUnleashService().isEnabled("veilarboppfolging.oppfolgingresolver.bruk_arena_direkte")) {
                hentOppfolgingstatusDirekteFraArena();
            } else {
                hentOppfolgingstatusFraVeilarbArena();

                // Fallbackløsning for å hente direkte fra Arena dersom bruker er under oppfølging, men veilarbarena
                // ikke har data på brukeren. Dette kan forekomme direkte etter registrering, før data har blitt
                // synkronisert fra Arena til veilarbarena.
                if (!arenaOppfolgingTilstand.isPresent() && oppfolging.isUnderOppfolging()) {
                    hentOppfolgingstatusDirekteFraArena();
                }
            }
        }
    }

    @SneakyThrows
    private void hentOppfolgingstatusDirekteFraArena() {
        if (!oppfolgingDirekteFraArena().isPresent()) {
            arenaOppfolgingTilstand = Try.of(() -> deps.getArenaOppfolgingService().hentArenaOppfolging(fnr))
                    .onFailure(e -> {
                        if (!(e instanceof NotFoundException)) {
                            log.warn("Feil fra Arena for aktørId: {}", aktorId, e);
                        }
                    })
                    .toJavaOptional().map(Either::right);
        }
    }

    private void hentOppfolgingstatusFraVeilarbArena() {
        if (!arenaOppfolgingTilstand.isPresent()) {
            Optional<VeilarbArenaOppfolging> veilarbArenaOppfolging =
                    deps.getOppfolgingsbrukerService().hentOppfolgingsbruker(fnr);

            arenaOppfolgingTilstand = veilarbArenaOppfolging.map(Either::left);

        }
    }

    private void sjekkReservasjonIKrrOgOppdaterOppfolging() {
        if (oppfolging.isUnderOppfolging()) {
            this.reservertIKrr = sjekkKrr();
            if (!manuell() && reservertIKrr) {
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
            this.reservertIKrr = false;
        }
    }

    @SneakyThrows
    private boolean sjekkKrr() {
        if (deps.getUnleashService().isEnabled("veilarboppfolging.dkif_rest")) {
            return sjekkDkifRest();
        } else {
            return sjekkDkifSoap();
        }
    }

    public boolean sjekkDkifRest() {
        UUID uuid = UUID.randomUUID();
        String callId = Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());

        String responseBody = RestUtils.withClient(c ->
                c.target("http://dkif.default.svc.nais.local/api/v1/personer/kontaktinformasjon")
                        .queryParam("inkluderSikkerDigitalPost", "false")
                        .request()
                        .header(AUTHORIZATION, "Bearer " + deps.getSystemUserTokenProvider().getToken())
                        .header("Nav-Personidenter", fnr)
                        .header("Nav-Call-Id", callId)
                        .header("Nav-Consumer-Id", APPLICATION_NAME)
                        .get(String.class));

        boolean kanVarsles = new JSONObject(responseBody)
                .getJSONObject("kontaktinfo")
                .getJSONObject(fnr)
                .getBoolean("kanVarsles");

        log.info("Dkif-response: {}: kanVarsles: {}", aktorId, kanVarsles);

        return !kanVarsles;
    }


    private boolean sjekkDkifSoap() {
        val req = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
        try {
            return of(deps.getDigitalKontaktinformasjonV1().hentDigitalKontaktinformasjon(req))
                    .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                    .map(WSKontaktinformasjon::getReservasjon)
                    .map("true"::equalsIgnoreCase)
                    .orElse(false);
        } catch (HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet |
                HentDigitalKontaktinformasjonPersonIkkeFunnet e) {
            log.info(e.getMessage(), e);
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    @SneakyThrows
    private void hentYtelseskontrakt() {
        val wsHentYtelseskontraktListeRequest = new WSHentYtelseskontraktListeRequest();
        wsHentYtelseskontraktListeRequest.setPersonidentifikator(fnr);
        this.ytelser = deps.getYtelseskontraktV3().hentYtelseskontraktListe(wsHentYtelseskontraktListeRequest);
    }

    private void hentArenaAktiviteter() {
        this.arenaAktiviteter = deps.getVeilarbaktivtetService().hentArenaAktiviteter(fnr);
    }

    private void avsluttKvpVedEnhetBytte() {
        Kvp gjeldendeKvp = deps.getKvpService().gjeldendeKvp(fnr);
        if (gjeldendeKvp == null) {
            return;
        }

        hentOppfolgingstatusFraArena();
        arenaOppfolgingTilstand().ifPresent(status -> {
            if (brukerHarByttetKontor(status, gjeldendeKvp)) {
                deps.getKvpService().stopKvpUtenEnhetSjekk(fnr, "KVP avsluttet automatisk pga. endret Nav-enhet", SYSTEM, this);
                FunksjonelleMetrikker.stopKvpDueToChangedUnit();
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

        @Inject
        private VeilarbAbacPepClient pepClient;

        @Inject
        private Transactor transactor;

        @Inject
        private AktorService aktorService;

        @Inject
        private OppfolgingRepository oppfolgingRepository;

        @Inject
        private ArenaOppfolgingService arenaOppfolgingService;

        @Inject
        private OppfolgingsbrukerService oppfolgingsbrukerService;

        @Inject
        private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

        @Inject
        private YtelseskontraktV3 ytelseskontraktV3;

        @Inject
        private VeilarbaktivtetService veilarbaktivtetService;

        @Inject
        private EskaleringsvarselService eskaleringsvarselService;

        @Inject
        private KvpRepository kvpRepository;

        @Inject
        private KvpService kvpService;

        @Inject
        private SystemUserTokenProvider systemUserTokenProvider;

        @Inject
        private UnleashService unleashService;

        @Inject
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
    String oppfolgingsenhet;
    LocalDate inaktiveringsdato;

    static ArenaOppfolgingTilstand fraArenaOppfolging(ArenaOppfolging arenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                arenaOppfolging.getFormidlingsgruppe(),
                arenaOppfolging.getServicegruppe(),
                arenaOppfolging.getOppfolgingsenhet(),
                arenaOppfolging.getInaktiveringsdato());
    }

    static ArenaOppfolgingTilstand fraArenaBruker(VeilarbArenaOppfolging veilarbArenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                veilarbArenaOppfolging.getFormidlingsgruppekode(),
                veilarbArenaOppfolging.getKvalifiseringsgruppekode(),
                veilarbArenaOppfolging.getNav_kontor(),
                Optional.ofNullable(veilarbArenaOppfolging.getIserv_fra_dato()).map(ZonedDateTime::toLocalDate).orElse(null)
        );
    }
}
