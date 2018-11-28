package no.nav.fo.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;

public class OppfoelgingsstatusV2Mock implements OppfoelgingsstatusV2 {

    @Override
    public void ping() {

    }

    @Override
    public no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(
            no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) {
        return null;
    }

}
