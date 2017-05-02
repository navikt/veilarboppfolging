package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static no.nav.fo.veilarbsituasjon.utils.UrlValidator.isInvalidUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
@Api(value= "Portefolje")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);


    private JmsTemplate endreVeilederQueue;
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private final PepClient pepClient;
    private List<VeilederTilordning> feilendeTilordninger;

    private static String webhookUrl = "";


    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, BrukerRepository brukerRepository, PepClient pepClient) {
        this.endreVeilederQueue = endreVeilederQueue;
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
        this.pepClient = pepClient;
    }

    @PUT
    @Path("/tilordning/webhook")
    public Response putWebhook(String callbackUrl) {
        if (webhookUrl.equals(callbackUrl)) {
            return Response.ok().build();
        } else if (isInvalidUrl(callbackUrl)) {
            LOG.warn("Callback-url er angitt på et ugyldig format: {callbackUrl} ");
            return Response.status(400).entity("Callback-url er angitt på et ugyldig format").build();
        }
        return createWebhook(callbackUrl);
    }

    @GET
    @Path("/tilordning")
    public Response getVeilederTilordninger(@QueryParam("since_id") String sinceId) {
        return Response.ok().entity(sinceId).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
        feilendeTilordninger = new ArrayList<>();
        try {

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
                response.setResultat("OK: Veiledere tilordnet");
                return Response.ok().entity(response).build();
            } else {
                response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
                return Response.ok().entity(response).build();
            }

        } catch (JMSException e) {
            return Response.serverError().entity("Kunne ikke legge brukere på kø").build();
        } catch (SQLException e) {
            return Response.serverError().entity("Kunne ikke skrive brukere til database").build();
        } catch (Exception e) {
            return Response.serverError().entity("Kunne ikke tildele veileder").build();
        }
    }

    @GET
    @Path("/sendalleveiledertilordninger")
    public Response getSendAlleVeiledertilordninger() {
        long start = System.currentTimeMillis();
        LOG.info("Sender alle veiledertilordninger");
        List<OppfolgingBruker> brukere = brukerRepository.hentAlleVeiledertilordninger();
        int sendt = 0;
        int feilet = 0;
        for (OppfolgingBruker bruker : brukere) {
            try {
                leggPaaKo(bruker);
                sendt++;
            } catch (Exception e) {
                feilet++;
            }
        }
        String status = String.format("Sending fullført. Sendt: %1$s/%2$s. Feilet: %3$s/%2$s. Tid brukt: %4$s ms",
                sendt, brukere.size(), feilet, System.currentTimeMillis() - start);

        if (feilet > 0) {
            LOG.error(status);
            return Response.serverError().entity(status).build();
        } else {
            LOG.info(status);
            return Response.ok().entity(status).build();
        }
    }

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(OppfolgingBruker bruker) throws SQLException, JMSException {
        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            leggPaaKo(bruker);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    private Response createWebhook(String url) {
        try {
            URI uri = new URI("tildelinger/webhook");
            webhookUrl = url;
            return Response.created(uri).build();
        } catch (URISyntaxException e) {
            LOG.warn("Ugyldig format på url for webhook");
            return Response.serverError().entity("Serveren oprettet url for webhook på et ugyldig format").build();
        }

    }

    private void leggPaaKo(OppfolgingBruker bruker) {
		endreVeilederQueue.send(messageCreator(bruker.toString()));
	}

    private void settVeilederDersomFraVeilederErOK(OppfolgingBruker bruker, VeilederTilordning tilordning) throws SQLException, JMSException {
        String eksisterendeVeileder = brukerRepository.hentVeilederForAktoer(bruker.getAktoerid());
        Boolean fraVeilederErOk = eksisterendeVeileder == null || eksisterendeVeileder.equals(tilordning.getFraVeilederId());

        if (fraVeilederErOk) {
            skrivTilDataBaseOgLeggPaaKo(bruker);
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