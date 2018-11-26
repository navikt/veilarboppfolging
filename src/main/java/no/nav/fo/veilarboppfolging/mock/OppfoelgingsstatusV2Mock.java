package no.nav.fo.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;

public class OppfoelgingsstatusV2Mock implements OppfoelgingsstatusV2 {

    @Override
    public void ping() {

    }

    @Override
    public no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) throws no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusPersonIkkeFunnet, no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusSikkerhetsbegrensning, no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusUgyldigInput {
        return null;
    }

}
