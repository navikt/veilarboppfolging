package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;

import static java.util.UUID.randomUUID;
import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

public class EndreVeilederService {

    private static final Logger LOG = getLogger(EndreVeilederService.class);

    @Autowired
    @Qualifier("endreveilederko")
    private JmsTemplate endreVeilederQueue;

    @Autowired
    private AktoerIdService aktoerIdService;

    @Transactional
    public void endreVeileder(VeilederTilordning veilederTilordning){
        LOG.debug("Starter tildeling av veileder");

        try {
            String aktoerId = aktoerIdService.findAktoerId(veilederTilordning.getFodselsnummerBruker());

            AktoerIdToVeileder aktoerIdToVeileder = new AktoerIdToVeileder()
                    .withVeileder(veilederTilordning.getIdentVeileder())
                    .withAktoerId(aktoerId);
            aktoerIdService.saveOrUpdateAktoerIdToVeileder(aktoerIdToVeileder);

            String endringsmeldingId = randomUUID().toString();
            endreVeilederQueue.send(messageCreator(aktoerIdToVeileder.toString(),endringsmeldingId));
        } catch (Exception e) {
            LOG.error("Kunne ikke tilordne veileder", e);
        }

        LOG.debug("Veileder tildelt");
    }
}
