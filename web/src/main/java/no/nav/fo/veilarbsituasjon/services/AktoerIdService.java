package no.nav.fo.veilarbsituasjon.services;


import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.fo.veilarbsituasjon.repository.AktoerIdToVeilederDAO;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.slf4j.LoggerFactory.getLogger;

@Transactional
public class AktoerIdService {

    private static final Logger LOG = getLogger(AktoerIdService.class);

    @Autowired
    private AktoerV2 aktoerV2;

    @Autowired
    private AktoerIdToVeilederDAO aktoerIdToVeilederDAO;

    public String findAktoerId(String fnr) {

        try {
            return aktoerV2.hentAktoerIdForIdent(
                    new WSHentAktoerIdForIdentRequest()
                            .withIdent(fnr)
            ).getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            LOG.error("AktoerId ikke funnet", e);
            return null;
        }
    }

    public AktoerIdToVeileder saveOrUpdateAktoerIdToVeileder(AktoerIdToVeileder aktoerIdToVeileder) {
        return aktoerIdToVeilederDAO.opprettEllerOppdaterAktoerIdToVeileder(aktoerIdToVeileder);
    }
}
