package no.nav.fo.veilarbsituasjon.services;


//import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import static org.slf4j.LoggerFactory.getLogger;

@Transactional
public class AktoerIdService {

    private static final Logger LOG = getLogger(AktoerIdService.class);

    private AktoerV2 aktoerV2;

    public AktoerIdService(AktoerV2 aktoerV2) {
        this.aktoerV2 = aktoerV2;
    }

    public String findAktoerId(String fnr) {

        try {
            HentAktoerIdForIdentRequest hentAktoerIdForIdentRequest = new HentAktoerIdForIdentRequest();
            hentAktoerIdForIdentRequest.setIdent(fnr);
            return aktoerV2.hentAktoerIdForIdent(hentAktoerIdForIdentRequest)
                    .getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            LOG.error("AktoerId ikke funnet", e);
            return null;
        }
    }
}
