package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import no.nav.apiapp.security.SubjectService;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.domain.Tilordning;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.brukerdialog.security.domain.IdentType.Systemressurs;
import static no.nav.common.auth.SubjectHandler.getIdentType;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value = "VeilederTilordningRessurs")
public class VeilederTilordningRessurs {

    private static final Logger LOG = getLogger(VeilederTilordningRessurs.class);

    private final AktorService aktorService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final VeilarbAbacPepClient pepClient;
    private final AutorisasjonService autorisasjonService;
    private FeedProducer<OppfolgingFeedDTO> oppfolgingFeed;
    private final SubjectService subjectService = new SubjectService();

    public VeilederTilordningRessurs(AktorService aktorService,
                                     VeilederTilordningerRepository veilederTilordningerRepository,
                                     VeilarbAbacPepClient pepClient,
                                     FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
                                     AutorisasjonService autorisasjonService
    ) {
        this.autorisasjonService = autorisasjonService;
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.pepClient = pepClient;
        this.oppfolgingFeed = oppfolgingFeed;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
        autorisasjonService.skalVereInternBruker();

        List<VeilederTilordning> feilendeTilordninger = new ArrayList<>();
        for (VeilederTilordning tilordning : tilordninger) {

            String innloggetVeilederId = "INGEN_IDENTTYPE_FUNNET";
            if (getIdentType().isPresent() && InternBruker.equals(getIdentType().get())) {
                innloggetVeilederId = SubjectHandler.getIdent().orElse("INGEN_VEILEDERIDENT_FUNNET");
            }

            Bruker bruker;
            try {
                bruker = lagBrukerFraFnr(tilordning.getBrukerFnr());
            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} årsak: Fant ikke aktørId for bruker", innloggetVeilederId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), e);
                continue;
            }

            try {
                pepClient.sjekkSkrivetilgangTilBruker(bruker);

                String aktoerId = bruker.getAktoerId();

                String eksisterendeVeileder = veilederTilordningerRepository.hentTilordningForAktoer(aktoerId);

                if (kanTilordneFraVeileder(eksisterendeVeileder, tilordning.getFraVeilederId())) {
                    if (nyVeilederHarTilgang(tilordning)) {
                        skrivTilDatabase(aktoerId, tilordning.getTilVeilederId());
                    } else {
                        LOG.info("Aktoerid {} kunne ikke tildeles. Ny veileder {} har ikke tilgang.", aktoerId, tilordning.getTilVeilederId());
                        feilendeTilordninger.add(tilordning);
                    }
                } else {
                    LOG.info("Aktoerid {} kunne ikke tildeles. Oppgitt fraVeileder {} er feil. Faktisk veileder: {}", aktoerId, tilordning.getFraVeilederId(), eksisterendeVeileder);
                    feilendeTilordninger.add(tilordning);
                }
            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                loggFeilOppfolging(e, tilordning, bruker, innloggetVeilederId);
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
            CompletableFuture.runAsync(() -> kallWebhook());
        }
        return Response.ok().entity(response).build();

    }

    @POST
    @Path("{fnr}/lestaktivitetsplan/")
    public void lestAktivitetsplan(@PathParam("fnr") String fnr) {

        Bruker bruker = lagBrukerFraFnr(fnr);

        autorisasjonService.skalVereInternBruker();
        pepClient.sjekkLesetilgangTilBruker(bruker);

        veilederTilordningerRepository.hentTilordnetVeileder(bruker.getAktoerId())
                .filter(Tilordning::isNyForVeileder)
                .filter(this::erVeilederFor)
                .map(FunksjonelleMetrikker::lestAvVeileder)
                .map(Tilordning::getAktorId)
                .map(veilederTilordningerRepository::markerSomLestAvVeileder)
                .ifPresent(i -> kallWebhook());
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

    private void loggFeilOppfolging(Exception e, VeilederTilordning tilordning, Bruker bruker, String innloggetVeilederId) {
        String fraVeilederId = tilordning.getFraVeilederId();
        String tilVeilederId = tilordning.getTilVeilederId();
        String aktoerId = bruker.getAktoerId();

        if (e instanceof NotAuthorizedException) {
            LOG.warn("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: request is not authorized", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else if (e instanceof PepException) {
            LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {}, årsak: kall til ABAC feilet", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else {
            LOG.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: ukjent årsak", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        }
    }

    private void skrivTilDatabase(String aktoerId, String veileder) {
        try {
            veilederTilordningerRepository.upsertVeilederTilordning(aktoerId, veileder);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", veileder, aktoerId));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", veileder, aktoerId), e);
            throw e;
        }
    }

    static boolean kanTilordneFraVeileder(String eksisterendeVeileder, String fraVeilederId) {
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeilederId);
    }

    private boolean nyVeilederHarTilgang(VeilederTilordning veilederTilordning) {
        return autorisasjonService.harVeilederSkriveTilgangTilFnr(veilederTilordning.getTilVeilederId(), veilederTilordning.getBrukerFnr());
    }

    private Bruker lagBrukerFraFnr(String fnr) {
        return Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet")));
    }


}