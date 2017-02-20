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
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/tilordneveileder")
@JWTInAuthorizationHeaderJAAS
@SessionTerminator
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);


    private JmsTemplate endreVelederQueue;
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;

    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, BrukerRepository brukerRepository) {
        this.endreVelederQueue = endreVeilederQueue;
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
    }

    @POST
    @Consumes("application/json")
    public Response postVeilederTilordninger(List<VeilederTilordning> veilederTilordninger) {
        try {
            for (int i = 0; i < veilederTilordninger.size(); i++) {
                String aktoerId = aktoerIdService.findAktoerId(veilederTilordninger.get(i).getFodselsnummerBruker());
                skrivTilDataBaseOgLeggPaaKo(new OppfolgingBruker()
                        .withVeileder(veilederTilordninger.get(i).getIdentVeileder())
                        .withAktoerid(aktoerId));
            }
            return Response.ok().entity("Veiledere tilordnet").build();
        } catch (JMSException e) {
            return Response.serverError().entity("Kunne ikke legge brukere på kø").build();
        } catch (SQLException e) {
            return Response.serverError().entity("Kunne ikke skrive brukere til database").build();
        } catch (Exception e) {
            return Response.serverError().entity("Kunne ikke oppdatere informasjon").build();
        }
    }

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(OppfolgingBruker bruker) throws SQLException, JMSException{
        String endringsmeldingId = randomUUID().toString();

        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            endreVelederQueue.send(messageCreator(bruker.toString(), endringsmeldingId));
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        }   catch(Exception e) {
            LOG.error(String.format("Veileder %s kunne ikke tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()),e);
            throw e;
        }
    }
}
