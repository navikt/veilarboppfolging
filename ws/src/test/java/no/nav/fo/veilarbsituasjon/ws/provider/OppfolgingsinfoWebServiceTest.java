package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingsinfoWebServiceTest {

    private static final String FNR_DUMMY = "FNR_DUMMY";
    private static final AktorId AKTORID_DUMMY = new AktorId("AKTORID_DUMMY");
    private static final String VEILEDER = "***REMOVED***";

    @Mock
    private OppfolgingService oppfolgingService;

    @InjectMocks
    private OppfolgingsinfoWebService oppfolgingsinfoWebService;


    @Test
    public void skalHenteOppfolgingsstatusMedFnr() throws Exception {
        when(oppfolgingService.hentOppfolgingsStatus(any(AktorId.class))).thenReturn(getTestData());
        OppfolgingsstatusRequest request = new OppfolgingsstatusRequest().withAktorId(AKTORID_DUMMY.getAktorId());
        OppfolgingsstatusResponse response = oppfolgingsinfoWebService.hentOppfolgingsstatus(request);

        assertThat(response.getWsOppfolgingsdata().getAktorId()).isEqualTo(AKTORID_DUMMY.getAktorId());
        assertThat(response.getWsOppfolgingsdata().getVeilederIdent()).isEqualTo(VEILEDER);
        assertThat(response.getWsOppfolgingsdata().isErUnderOppfolging()).isTrue();

    }

    private OppfolgingStatusData getTestData() {
        return new OppfolgingStatusData()
                .setVeilederId(VEILEDER)
                .setUnderOppfolging(true);
    }


}