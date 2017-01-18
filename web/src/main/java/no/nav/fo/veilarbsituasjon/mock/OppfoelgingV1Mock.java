package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

public class OppfoelgingV1Mock implements OppfoelgingPortType {
    @Override
    public WSHentOppfoelgingskontraktListeResponse hentOppfoelgingskontraktListe(WSHentOppfoelgingskontraktListeRequest request) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        return null;
    }

    @Override
    public void ping() {

    }
}
