package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1;

import no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;

public interface OppfolgingsinfoV1 {
    OppfolgingsstatusResponse hentOppfolgingsstatus(OppfolgingsstatusRequest request);

    void ping();
}
