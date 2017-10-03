package no.nav.fo.veilarbsituasjon.services;


import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
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
            return aktoerV2.hentAktoerIdForIdent(
                    new WSHentAktoerIdForIdentRequest().withIdent(fnr)
            ).getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            LOG.error("AktoerId ikke funnet for fnr {}", fnr, e);
            return null;
        }
    }

    public String findFnr(String aktoerId) {
        try {
            return aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerId)
            ).getIdent();
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            LOG.error("FNR ikke funnet", e);
            return null;
        }
    }
}
