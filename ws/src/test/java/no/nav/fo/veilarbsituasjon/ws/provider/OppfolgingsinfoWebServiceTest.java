package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.AktorId;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.fo.veilarbsituasjon.services.SituasjonResolver;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingsinfoWebServiceTest {

    private static final String FNR_DUMMY = "FNR_DUMMY";
    private static final AktorId AKTORID_DUMMY = new AktorId("AKTORID_DUMMY");
    private static final String VEILEDER = "***REMOVED***";

    @Mock
    private SituasjonOversiktService situasjonOversiktService;

    @InjectMocks
    private OppfolgingsinfoWebService oppfolgingsinfoWebService;


    @Test
    public void skalHenteOppfolgingsstatusMedFnr() throws Exception {
        when(situasjonOversiktService.hentOppfolgingsStatus(any(AktorId.class))).thenReturn(getTestData());
        OppfolgingsstatusRequest request = new OppfolgingsstatusRequest().withAktorId(AKTORID_DUMMY.getAktorId());
        OppfolgingsstatusResponse response = oppfolgingsinfoWebService.hentOppfolgingsstatus(request);

        assertThat(response.getAktorId()).isEqualTo(AKTORID_DUMMY.getAktorId());
        assertThat(response.getVeilederIdent()).isEqualTo(VEILEDER);
        assertThat(response.isErUnderOppfolging()).isTrue();

    }

    private OppfolgingStatusData getTestData() {
        return new OppfolgingStatusData()
                .setVeilederId(VEILEDER)
                .setUnderOppfolging(true);
    }


}