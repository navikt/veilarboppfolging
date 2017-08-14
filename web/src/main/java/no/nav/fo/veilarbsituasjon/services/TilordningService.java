package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.fo.veilarbsituasjon.utils.JmsUtil.messageCreator;
import static org.slf4j.LoggerFactory.getLogger;

public class TilordningService {
    private static final Logger LOG = getLogger(TilordningService.class);

    private final JmsTemplate endreVeilederQueue;
    private final BrukerRepository brukerRepository;

    public TilordningService(JmsTemplate endreVeilederQueue, BrukerRepository brukerRepository) {
        this.endreVeilederQueue = endreVeilederQueue;
        this.brukerRepository = brukerRepository;
    }

    @Transactional
    public void skrivTilDataBaseOgLeggPaaKo(String aktoerId, String tilVeileder) {
        OppfolgingBruker bruker = new OppfolgingBruker().setAktoerid(aktoerId).setVeileder(tilVeileder);
        try {
            brukerRepository.leggTilEllerOppdaterBruker(bruker);
            leggPaaKo(bruker);
            LOG.debug(String.format("Veileder %s tilordnet aktoer %s", bruker.getVeileder(), bruker.getAktoerid()));
        } catch (Exception e) {
            LOG.error(String.format("Kunne ikke tilordne veileder %s til aktoer %s", bruker.getVeileder(), bruker.getAktoerid()), e);
            throw e;
        }
    }

    public void leggPaaKo(OppfolgingBruker bruker) {
        endreVeilederQueue.send(messageCreator(bruker.toString()));
    }
}
