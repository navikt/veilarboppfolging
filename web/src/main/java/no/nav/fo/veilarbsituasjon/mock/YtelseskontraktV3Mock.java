package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;


public class YtelseskontraktV3Mock implements YtelseskontraktV3 {
    @Override
    public void ping() {

    }

    @Override
    public WSHentYtelseskontraktListeResponse hentYtelseskontraktListe(WSHentYtelseskontraktListeRequest request) throws HentYtelseskontraktListeSikkerhetsbegrensning {
        return null;
    }
}
