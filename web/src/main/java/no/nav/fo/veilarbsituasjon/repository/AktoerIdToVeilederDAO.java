package no.nav.fo.veilarbsituasjon.repository;

import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import static org.slf4j.LoggerFactory.getLogger;

@Transactional
public class AktoerIdToVeilederDAO extends AbstractDAO<AktoerIdToVeileder> {

    private static final Logger LOG = getLogger(AktoerIdToVeilederDAO.class);

    @Override
    protected Logger log() { return LOG; }

    public AktoerIdToVeileder opprettEllerOppdaterAktoerIdToVeileder(AktoerIdToVeileder aktoerIdToVeileder) {
        return saveOrUpdate(aktoerIdToVeileder);
    }
}
