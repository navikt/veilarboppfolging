package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.jms.JMSException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

@RestController
@RequestMapping("/tilordneveileder")
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

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<?> postVeilederTilordninger(@RequestBody List<VeilederTilordning> veilederTilordninger, HttpServletResponse response) {
        try {
            for (int i = 0; i < veilederTilordninger.size(); i++) {
                String aktoerId = aktoerIdService.findAktoerId(veilederTilordninger.get(i).getFodselsnummerBruker());
                skrivTilDataBaseOgLeggPaaKo(new OppfolgingBruker()
                        .withVeileder(veilederTilordninger.get(i).getIdentVeileder())
                        .withAktoerid(aktoerId));
            }
            return new ResponseEntity<>("Veiledere tilordnet", HttpStatus.OK);
        } catch (JMSException e) {
            return new ResponseEntity<>("Kunne ikke legge brukere på kø ", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (SQLException e) {
            return new ResponseEntity<>("Kunne ikke skrive brukere til database ", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Kunne ikke oppdatere informasjon ", HttpStatus.INTERNAL_SERVER_ERROR);
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
