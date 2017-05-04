package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import javaslang.control.Try;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import no.nav.fo.veilarbsituasjon.exception.HttpNotSupportedException;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;
import static no.nav.fo.veilarbsituasjon.utils.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value = "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);

    @Inject
    private TilordningService tilordningService;

    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private final PepClient pepClient;
    private List<VeilederTilordning> feilendeTilordninger;

    private static String webhookUrl = "";


    public PortefoljeRessurs(AktoerIdService aktoerIdService, BrukerRepository brukerRepository, PepClient pepClient) {
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/tilordninger/webhook")
    public Response getWebhook() {
        return Response.ok().entity(webhookUrl).build();
    }

    @PUT
    @Path("/tilordninger/webhook")
    public Response putWebhook(String callbackUrl) {
        if (webhookUrl.equals(callbackUrl)) {
            return Response.ok().build();
        }

        Try.of(() -> {
            validateUrl(callbackUrl);
            webhookUrl = callbackUrl;
            URI uri = new URI("tilordninger/webhook");
            return Response.created(uri).build();

        }).recover(e -> Match(e).of(
                Case(instanceOf(URISyntaxException.class),
                        Response.serverError().entity("Det skjedde en feil web opprettelsen av webhook").build()),
                Case(instanceOf(MalformedURLException.class),
                        Response.status(400).entity("Feil format på callback-url").build()),
                Case(instanceOf(HttpNotSupportedException.class),
                        Response.status(400).entity("Angitt url for webhook må være HTTPS").build())
        ));

        return Response.status(500).entity("Det har skjedd en feil på serveren").build();
    }

    @GET
    @Path("/tilordninger")
    @Produces("application/json")
    public Response getTilordninger(@QueryParam("since_id") String sinceId) {
        LinkedList<Tilordning> tilordninger = tilordningService.hentTilordninger(sinceId);
        return Response.ok().entity(tilordninger).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
        feilendeTilordninger = new ArrayList<>();

        for (VeilederTilordning tilordning : tilordninger) {

            final String fnr = tilordning.getBrukerFnr();
            pepClient.isServiceCallAllowed(fnr);
            String aktoerId = aktoerIdService.findAktoerId(fnr);

            OppfolgingBruker bruker = new OppfolgingBruker()
                    .setVeileder(tilordning.getTilVeilederId())
                    .setAktoerid(aktoerId);

            settVeilederDersomFraVeilederErOK(bruker, tilordning);
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse()
                .setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("Veiledere tilordnet!");
            activateWebhook();
            return Response.ok().entity(response).build();
        } else {
            response.setResultat("Noen brukere kunne ikke tilordnes en veileder.");
            return Response.ok().entity(response).build();
        }
    }

    private void activateWebhook() {
        Try.of(() -> {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write("foo");
            out.close();
            return connection.getInputStream();
        }).onFailure(e -> {
            LOG.warn("Det skjedde en feil ved aktivering av webhook", e.getMessage());
        });
    }

    @Transactional
    private void skrivTilDatabase(OppfolgingBruker bruker) {
        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    private void settVeilederDersomFraVeilederErOK(OppfolgingBruker bruker, VeilederTilordning tilordning) {
        String eksisterendeVeileder = brukerRepository.hentVeilederForAktoer(bruker.getAktoerid());
        Boolean fraVeilederErOk = eksisterendeVeileder == null || eksisterendeVeileder.equals(tilordning.getFraVeilederId());

        if (fraVeilederErOk) {
            skrivTilDatabase(bruker);
        } else {
            feilendeTilordninger.add(tilordning);
            LOG.info("Aktoerid {} kunne ikke tildeles ettersom fraVeileder er feil", bruker.getAktoerid());
        }
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}