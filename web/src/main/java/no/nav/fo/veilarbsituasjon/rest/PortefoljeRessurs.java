package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.security.jwt.filter.JWTInAuthorizationHeaderJAAS;
import no.nav.fo.security.jwt.filter.SessionTerminator;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
    @Path("/tilordneveileder")
    @JWTInAuthorizationHeaderJAAS
    @SessionTerminator
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

            if (feilendeTilordninger.isEmpty()) {
                return Response.ok().entity("Veiledere tilordnet").build();
            } else {
                return Response.ok().entity(feilendeTilordninger).build();
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
            for (int i = 0; i < brukere.size(); i++) {
                skrivTilDataBaseOgLeggPaaKo(brukere.get(i));
            }
            return Response.ok().entity("Alle veiledertilordninger sendt").build();
        } catch(Exception e) {
            LOG.error("Kunne ikke legge alle veiledertilordninge på ko");
            return Response.serverError().entity("Kunne ikke sende alle veiledertilordninger").build();
        }
    }

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(OppfolgingBruker bruker) throws SQLException, JMSException {
        String endringsmeldingId = randomUUID().toString();

        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            endreVeilederQueue.send(messageCreator(bruker.toString(), endringsmeldingId));
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
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