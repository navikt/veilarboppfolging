package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.OppfolgingsinfoV1;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.fo.test.smoketest.SmokeTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OppfolgingsinfoWebServiceTest {

    public static final String PROXY_USERNAME_ALIAS = "srvveilarbsituasjonproxy";

    private OppfolgingsinfoV1 oppfolgingsinfoV1;

    @BeforeAll
    public static void setUp() throws Exception {
        System.getProperties().load(OppfolgingsinfoWebServiceTest.class.getResourceAsStream("/smoketest.properties"));
    }

    @BeforeEach
    public void before() throws Exception {
        setupSystemUser(PROXY_USERNAME_ALIAS);
        setupOpenAmSubject();
        oppfolgingsinfoV1 = new CXFClient<>(OppfolgingsinfoV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address("https://app-" + getMiljo() + ".adeo.no/veilarbsituasjon-ws/ws/sit")
                .configureStsForExternalSSO()
                .build();
    }

    @Test
    public void hentOppfolgingsstatus() throws Exception {
        OppfolgingsstatusRequest req = new OppfolgingsstatusRequest();
        req.setAktorId("***REMOVED***");
        OppfolgingsstatusResponse res = oppfolgingsinfoV1.hentOppfolgingsstatus(req);
        assertNotNull(res.getAktorId());
    }


}
