package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("")
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);


    private JmsTemplate endreVeilederQueue;
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;
    private List<VeilederTilordning> feilendeTilordninger;


    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, BrukerRepository brukerRepository) {
        this.endreVeilederQueue = endreVeilederQueue;
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/tilordneveileder")
    public Response postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
        feilendeTilordninger = new ArrayList<>();
        try {

            for (VeilederTilordning tilordning : tilordninger) {
                String aktoerId = aktoerIdService.findAktoerId(tilordning.getBrukerFnr());
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
        List<OppfolgingBruker> brukere = brukerRepository.hentAlleVeiledertilordninger();
        try {
            for (OppfolgingBruker bruker : brukere) {
                leggPaaKo(bruker);
            }
            return Response.ok().entity("Alle veiledertilordninger sendt").build();
        } catch(Exception e) {
            LOG.error("Kunne ikke legge alle veiledertilordninge på ko");
            return Response.serverError().entity("Kunne ikke sende alle veiledertilordninger").build();
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

	private void leggPaaKo(OppfolgingBruker bruker) {
		endreVeilederQueue.send(messageCreator(bruker.toString(), randomUUID().toString()));
	}

    private void settVeilederDersomFraVeilederErOK(OppfolgingBruker bruker, VeilederTilordning tilordning) throws SQLException, JMSException {
        String eksisterendeVeileder = brukerRepository.hentVeilederForAktoer(bruker.getAktoerid());
        Boolean fraVeilederErOk = eksisterendeVeileder == null || eksisterendeVeileder.equals(tilordning.getFraVeilederId());

        if (fraVeilederErOk) {
            skrivTilDataBaseOgLeggPaaKo(bruker);
        } else {
            feilendeTilordninger.add(tilordning);
            LOG.info("Aktoerid %s kunne ikke tildeles ettersom fraVeileder er feil", bruker.getAktoerid());
        }
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}