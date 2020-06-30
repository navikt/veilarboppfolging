package no.nav.veilarboppfolging.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.SubjectService;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.db.VeilederHistorikkRepository;
import no.nav.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.Tilordning;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.domain.VeilederTilordning;
import no.nav.veilarboppfolging.services.AutorisasjonService;
import no.nav.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.jdbc.Transactor;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static no.nav.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value = "VeilederTilordningRessurs")
@Slf4j
public class VeilederTilordningRessurs {

    private static final Logger LOG = getLogger(VeilederTilordningRessurs.class);

    private final AktorService aktorService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final PepClient pepClient;
    private final AutorisasjonService autorisasjonService;
    private final Timer timer;
    private FeedProducer<OppfolgingFeedDTO> oppfolgingFeed;
    private final SubjectService subjectService = new SubjectService();
    private final OppfolgingRepository oppfolgingRepository;
    private final VeilederHistorikkRepository veilederHistorikkRepository;
    private final Transactor transactor;
    private final OppfolgingStatusKafkaProducer kafka;

    public VeilederTilordningRessurs(AktorService aktorService,
                                     VeilederTilordningerRepository veilederTilordningerRepository,
                                     PepClient pepClient,
                                     FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
                                     AutorisasjonService autorisasjonService,
                                     OppfolgingRepository oppfolgingRepository,
                                     VeilederHistorikkRepository veilederHistorikkRepository,
                                     Transactor transactor,
                                     OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer) {
        this.autorisasjonService = autorisasjonService;
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.pepClient = pepClient;
        this.oppfolgingFeed = oppfolgingFeed;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
        this.transactor = transactor;
        this.timer = MetricsFactory.createTimer("veilarboppfolging.veiledertilordning");
        this.kafka = oppfolgingStatusKafkaProducer;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {

        timer.start();

        autorisasjonService.skalVereInternBruker();
        String innloggetVeilederId = SubjectHandler.getIdent().orElseThrow(IllegalStateException::new);

        log.info("{} Prøver å tildele veileder", innloggetVeilederId);

        List<VeilederTilordning> feilendeTilordninger = new ArrayList<>();
        for (VeilederTilordning tilordning : tilordninger) {

            tilordning.setInnloggetVeilederId(innloggetVeilederId);

            try {
                String aktorId = getAktorIdOrElseThrow(aktorService, tilordning.getBrukerFnr()).getAktorId();

                pepClient.sjekkSkrivetilgangTilAktorId(aktorId);

                tilordning.setAktoerId(aktorId);

                String eksisterendeVeileder = veilederTilordningerRepository.hentTilordningForAktoer(aktorId);

                feilendeTilordninger = tildelVeileder(feilendeTilordninger, tilordning, aktorId, eksisterendeVeileder);

            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                loggFeilOppfolging(e, tilordning);
            }
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse().setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("OK: Veiledere tilordnet");
        } else {
            response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
        }
        if (tilordninger.size() > feilendeTilordninger.size()) {
            //Kaller denne asynkront siden resultatet ikke er interessant og operasjonen tar litt tid.
            CompletableFuture.runAsync(this::kallWebhook);
        }

        timer.stop();
        timer.report();

        return Response.ok().entity(response).build();
    }

    private List<VeilederTilordning> tildelVeileder(List<VeilederTilordning> feilendeTilordninger, VeilederTilordning tilordning, String aktoerId, String eksisterendeVeileder) {
        if (kanTilordneVeileder(eksisterendeVeileder, tilordning)) {
            if (nyVeilederHarTilgang(tilordning)) {
                skrivTilDatabase(aktoerId, tilordning.getTilVeilederId());
            } else {
                LOG.info("Aktoerid {} kunne ikke tildeles. Ny veileder {} har ikke tilgang.", aktoerId, tilordning.getTilVeilederId());
                feilendeTilordninger.add(tilordning);
            }
        } else {
            LOG.info("Aktoerid {} kunne ikke tildeles. Oppgitt fraVeileder {} er feil eller tilVeileder {} er feil. Faktisk veileder: {}",
                    aktoerId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), eksisterendeVeileder);
            feilendeTilordninger.add(tilordning);
        }

        return feilendeTilordninger;
    }

    @POST
    @Path("{fnr}/lestaktivitetsplan/")
    public void lestAktivitetsplan(@PathParam("fnr") String fnr) {

        String aktorId = getAktorIdOrElseThrow(aktorService, fnr).getAktorId();

        autorisasjonService.skalVereInternBruker();
        pepClient.sjekkLesetilgangTilAktorId(aktorId);

        veilederTilordningerRepository.hentTilordnetVeileder(aktorId)
                .filter(Tilordning::isNyForVeileder)
                .filter(this::erVeilederFor)
                .map(FunksjonelleMetrikker::lestAvVeileder)
                .map(Tilordning::getAktorId)
                .map(veilederTilordningerRepository::markerSomLestAvVeileder)
                .ifPresent(i -> kallWebhook());
    }

    private void loggFeilOppfolging(Exception e, VeilederTilordning tilordning) {

        String fraVeilederId = tilordning.getFraVeilederId();
        String tilVeilederId = tilordning.getTilVeilederId();
        String innloggetVeilederId = tilordning.getInnloggetVeilederId();
        String aktoerId = tilordning.getAktoerId();

        if (e instanceof NotAuthorizedException) {
            LOG.warn("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: request is not authorized", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else if (e instanceof PepException) {
            LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {}, årsak: kall til ABAC feilet", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else if (e instanceof IllegalArgumentException) {
            LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} årsak: Fant ikke aktørId for bruker", innloggetVeilederId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), e);
        } else {
            LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: ukjent årsak", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        }
    }


    private boolean erVeilederFor(Tilordning tilordning) {
        return subjectService.getUserId()
                .map((userId) -> userId.equals(tilordning.getVeilederId()))
                .orElse(false);
    }

    private void kallWebhook() {
        try {
            //Venter for å gi tid til å populere ID-er i feeden
            Thread.sleep(OppfolgingFeedRepository.INSERT_ID_INTERVAL);
            oppfolgingFeed.activateWebhook();
        } catch (Exception e) {
            // Logger feilen, men bryr oss ikke om det. At webhooken feiler påvirker ikke funksjonaliteten
            // men gjør at endringen kommer senere inn i portefølje
            LOG.warn("Webhook feilet", e);
        }
    }

    public void skrivTilDatabase(String aktoerId, String veileder) {
        transactor.inTransaction(() -> {
            veilederTilordningerRepository.upsertVeilederTilordning(aktoerId, veileder);
            veilederHistorikkRepository.insertTilordnetVeilederForAktorId(aktoerId, veileder);
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);
            kafka.send(new AktorId(aktoerId));
        });

        LOG.debug(String.format("Veileder %s tilordnet aktoer %s", veileder, aktoerId));
    }

    static boolean kanTilordneVeileder(String eksisterendeVeileder, VeilederTilordning veilederTilordning) {
        return eksisterendeVeileder == null || validerVeilederTilordning(eksisterendeVeileder, veilederTilordning);
    }

    static boolean validerVeilederTilordning(String eksisterendeVeileder, VeilederTilordning veilederTilordning) {
        return eksisterendeVeilederErSammeSomFra(eksisterendeVeileder, veilederTilordning.getFraVeilederId()) &&
                tildelesTilAnnenVeileder(eksisterendeVeileder, veilederTilordning.getTilVeilederId());
    }

    static boolean eksisterendeVeilederErSammeSomFra(String eksisterendeVeileder, String fraVeileder) {
        return eksisterendeVeileder.equals(fraVeileder);
    }

    static boolean tildelesTilAnnenVeileder(String eksisterendeVeileder, String tilVeileder) {
        return !eksisterendeVeileder.equals(tilVeileder);
    }

    private boolean nyVeilederHarTilgang(VeilederTilordning veilederTilordning) {
        return autorisasjonService.harVeilederSkriveTilgangTilFnr(veilederTilordning.getTilVeilederId(), veilederTilordning.getBrukerFnr());
    }
}
