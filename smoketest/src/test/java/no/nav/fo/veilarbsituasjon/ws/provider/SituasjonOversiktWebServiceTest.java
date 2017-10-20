package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.BehandleSituasjonV1;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentVilkaarSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static no.nav.fo.test.smoketest.SmokeTestUtils.getMiljo;
import static no.nav.fo.test.smoketest.SmokeTestUtils.setupOpenAmSubject;
import static no.nav.fo.test.smoketest.SmokeTestUtils.setupSystemUser;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public class SituasjonOversiktWebServiceTest {

    public static final String PROXY_USERNAME_ALIAS = "srvveilarbsituasjonproxy";

    private BehandleSituasjonV1 behandleSituasjonV1;

    @BeforeAll
    public static void setUp() throws Exception {
        System.getProperties().load(SituasjonOversiktWebServiceTest.class.getResourceAsStream("/smoketest.properties"));
    }

    @BeforeEach
    public void before() throws Exception {
        setupSystemUser(PROXY_USERNAME_ALIAS);
        setupOpenAmSubject();
        behandleSituasjonV1 = new CXFClient<>(BehandleSituasjonV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address("https://app-" + getMiljo() + ".adeo.no/veilarbsituasjon-ws/ws/Situasjon")
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
