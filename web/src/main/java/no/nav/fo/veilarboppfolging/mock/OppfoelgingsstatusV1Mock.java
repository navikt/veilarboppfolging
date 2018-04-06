package no.nav.fo.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusUgyldigInput;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse;

public class OppfoelgingsstatusV1Mock implements OppfoelgingsstatusV1 {

    public static final String AKTIV_STATUS = "Aktiv";

    @Override
    public void ping() {

    }

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest request) throws HentOppfoelgingsstatusPersonIkkeFunnet, HentOppfoelgingsstatusSikkerhetsbegrensning, HentOppfoelgingsstatusUgyldigInput {
        return null;
    }
}
