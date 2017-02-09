package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.fo.veilarbsituasjon.repository.AktoerIdToVeilederDAO;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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
    private AktoerIdToVeilederDAO aktoerIdToVeilederDAO;

    public PortefoljeRessurs(JmsTemplate endreVeilederQueue, AktoerIdService aktoerIdService, AktoerIdToVeilederDAO aktoerIdToVeilederDAO) {
        this.endreVelederQueue = endreVeilederQueue;
        this.aktoerIdService = aktoerIdService;
        this.aktoerIdToVeilederDAO = aktoerIdToVeilederDAO;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<?> postVeilederTilordninger(@RequestBody List<VeilederTilordning> veilederTilordninger, HttpServletResponse response) {
        try {
            for (int i = 0; i < veilederTilordninger.size(); i++) {
                String aktoerId = aktoerIdService.findAktoerId(veilederTilordninger.get(i).getFodselsnummerBruker());
                skrivTilDataBaseOgLeggPaaKo(new AktoerIdToVeileder()
                        .withVeileder(veilederTilordninger.get(i).getIdentVeileder())
                        .withAktoerId(aktoerId));
            }
            return new ResponseEntity<>("Veiledere tilordnet", HttpStatus.OK);
        } catch ( Exception e) {
            return new ResponseEntity<>("Kunne ikke tilordne veileder", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    private void skrivTilDataBaseOgLeggPaaKo(AktoerIdToVeileder aktoerIdToVeileder) {
    String endringsmeldingId = randomUUID().toString();

    try {
        aktoerIdToVeilederDAO.opprettEllerOppdaterAktoerIdToVeileder(aktoerIdToVeileder);
        endreVelederQueue.send(messageCreator(aktoerIdToVeileder.toString(), endringsmeldingId));
        LOG.debug(String.format("Veileder %s tilordnet aktoer %s", aktoerIdToVeileder.getVeileder(), aktoerIdToVeileder.getAktoerid()));
    }   catch(Exception e) {
        LOG.error(String.format("Veileder %s kunne ikke tilordnet aktoer %s", aktoerIdToVeileder.getVeileder(), aktoerIdToVeileder.getAktoerid()),e);
    }
    }
}
