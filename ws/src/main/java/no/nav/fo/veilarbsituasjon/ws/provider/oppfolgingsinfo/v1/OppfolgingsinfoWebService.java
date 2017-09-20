package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1;

import no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;

import javax.jws.WebService;

@WebService
public class OppfolgingsinfoWebService implements OppfolgingsinfoV1 {

    @Override
    public OppfolgingsstatusResponse hentOppfolgingsstatus(OppfolgingsstatusRequest request) {
        return new OppfolgingsstatusResponse();
    }

    @Override
    public void ping() {};
}
