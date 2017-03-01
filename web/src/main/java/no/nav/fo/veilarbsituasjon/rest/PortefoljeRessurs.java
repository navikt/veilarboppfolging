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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/tilordneveileder")
@JWTInAuthorizationHeaderJAAS
@SessionTerminator
public class PortefoljeRessurs {

    private static final Logger LOG = getLogger(PortefoljeRessurs.class);


    private JmsTemplate endreVeilederQueue;
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;

    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, BrukerRepository brukerRepository) {
        this.endreVeilederQueue = endreVeilederQueue;
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
    }

    @POST
    @Consumes("application/json")
    public Response postVeilederTilordninger(VeilederTilordning tilordning) {
        try {

            List<String> aktoerIds = tilordning
                    .getBrukere()
                    .stream()
                    .map(fnr -> aktoerIdService.findAktoerId(fnr))
                    .collect(toList());

            for (String aktoerId : aktoerIds) {
                OppfolgingBruker bruker = new OppfolgingBruker()
                        .setVeileder(tilordning.getTilVeileder())
                        .setAktoerid(aktoerId);

                skrivTilDataBaseOgLeggPaaKo(tilordning.getFraVeileder(), bruker);
            }

            return Response.ok().entity("Veiledere tilordnet").build();
        } catch (JMSException e) {
            return Response.serverError().entity("Kunne ikke legge brukere på kø").build();
        } catch (SQLException e) {
            return Response.serverError().entity("Kunne ikke skrive brukere til database").build();
        } catch (Exception e) {
            return Response.serverError().entity("Kunne ikke tildele veileder").build();
        }
    }

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(String fraVeileder, OppfolgingBruker bruker) throws SQLException, JMSException {
        String endringsmeldingId = randomUUID().toString();

        try {
            String veilederFraDb = brukerRepository.hentVeilederForAktoer(bruker.getAktoerid());
            if (kanSetteNyVeileder(fraVeileder, bruker.getVeileder(), veilederFraDb)) {
                brukerRepository.leggTilEllerOppdaterBruker(bruker);
                endreVeilederQueue.send(messageCreator(bruker.toString(), endringsmeldingId));
                LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
            } else {
                LOG.error(String.format("Forrige veileder %s samsvarer ikke med gjeldende veileder i database for aktoer %s", fraVeileder, bruker.getAktoerid()));
                throw new RuntimeException(String.format("Forrige veileder %s samsvarer ikke med gjeldende veileder i database for aktoer %s", fraVeileder, bruker.getAktoerid()));
            }
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    static boolean kanSetteNyVeileder(String fraVeileder, String tilVeileder, String eksisterendeVeileder) {
        if (tilVeileder == null) {
            return false;
        }
        return eksisterendeVeileder == null || eksisterendeVeileder.equals(fraVeileder);
    }
}
