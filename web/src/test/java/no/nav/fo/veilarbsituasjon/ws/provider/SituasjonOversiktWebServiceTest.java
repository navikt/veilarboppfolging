package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.fo.veilarbsituasjon.StartJetty;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.BehandleSituasjonV1;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SituasjonOversiktWebServiceTest {

    private static Jetty jetty;

    @BeforeAll
    public static void setUp() throws Exception {
        System.setProperty("lokal.database", Boolean.TRUE.toString());
        jetty = StartJetty.startJetty(32548);
        jetty.start();
    }

    @Test
    public void test() throws HentOppfoelgingsstatusSikkerhetsbegrensning {
        BehandleSituasjonV1 behandleSituasjonV1 = new CXFClient<>(BehandleSituasjonV1.class)
                .address("http://localhost:32548/veilarbsituasjon/ws/Situasjon")
                .build();

        HentOppfoelgingsstatusRequest req = new HentOppfoelgingsstatusRequest();
        req.setPersonident("***REMOVED***");
        HentOppfoelgingsstatusResponse res = behandleSituasjonV1.hentOppfoelgingsstatus(req);
        assertNotNull(res.getOppfoelgingsstatus());
    }

    @AfterAll
    public static void tearDown() {
        jetty.stop.run();
    }


}
