package no.nav.fo.veilarboppfolging.ws.provider;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.BehandleOppfolgingV1;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentVilkaarSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.meldinger.HentVilkaarRequest;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.meldinger.HentVilkaarResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public class OppfolgingWebServiceTest {

    private BehandleOppfolgingV1 behandleSituasjonV1;

    @BeforeAll
    public static void setUp() throws Exception {
        System.getProperties().load(OppfolgingWebServiceTest.class.getResourceAsStream("/smoketest.properties"));
    }

    @BeforeEach
    public void before() throws Exception {
        behandleSituasjonV1 = new CXFClient<>(BehandleOppfolgingV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address("https://app-" + FasitUtils.getDefaultEnvironment() + ".adeo.no/veilarboppfolging-ws/ws/Oppfolging")
                .configureStsForExternalSSO()
                .build();
    }

    @Test
    public void hentOppfolgingStatus() throws HentOppfoelgingsstatusSikkerhetsbegrensning {
        HentOppfoelgingsstatusRequest req = new HentOppfoelgingsstatusRequest();
        req.setPersonident("***REMOVED***");
        HentOppfoelgingsstatusResponse res = behandleSituasjonV1.hentOppfoelgingsstatus(req);
        assertNotNull(res.getOppfoelgingsstatus());
    }

    @Test
    public void hentVilkar() throws HentVilkaarSikkerhetsbegrensning {
        HentVilkaarRequest req = new HentVilkaarRequest();
        HentVilkaarResponse hentVilkaarResponse = behandleSituasjonV1.hentVilkaar(req);
        System.out.println(hentVilkaarResponse.getVilkaarstekst());
    }

}
