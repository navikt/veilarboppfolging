package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo.v1;


import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.OppfolgingsinfoV1;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;

import javax.inject.Inject;
import javax.jws.WebService;

@WebService
@SoapTjeneste("/sit")
public class OppfolgingsinfoWebService implements OppfolgingsinfoV1 {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Override
    public OppfolgingsstatusResponse hentOppfolgingsstatus(OppfolgingsstatusRequest request) {
        OppfolgingStatusData oppfolgingStatusData = null;
        try {
            oppfolgingStatusData = situasjonOversiktService.hentOppfolgingsStatus(request.getAktorId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        OppfolgingsstatusResponse response = new OppfolgingsstatusResponse();
        response.setAktorId(request.getAktorId());
        response.setErUnderOppfolging(oppfolgingStatusData.isUnderOppfolging());
        response.setVeilederIdent(oppfolgingStatusData.getVeilederId());
        return response;
    }

    @Override
    public void ping() {

    }
}
