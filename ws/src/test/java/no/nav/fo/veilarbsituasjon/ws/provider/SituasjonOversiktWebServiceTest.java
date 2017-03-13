package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.BehandleSituasjonV1;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentVilkaarSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbsituasjon.ws.StartJettyWS.jettyBuilder;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SituasjonOversiktWebServiceTest {

    private static Jetty jetty;
    private BehandleSituasjonV1 behandleSituasjonV1;

    @BeforeAll
    public static void setUp() throws Exception {
        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setProperty("lokal.database", Boolean.TRUE.toString());
        jetty = jettyBuilder(32548).loadProperties("/situasjonOversiktWebServiceTest.properties").buildJetty();
        jetty.start();
    }

    @BeforeEach
    public void before() {
        behandleSituasjonV1 = new CXFClient<>(BehandleSituasjonV1.class)
                .address("https://localhost:32549/veilarbsituasjon-ws/ws/Situasjon")
                .configureStsForSystemUser()
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

    @AfterAll
    public static void tearDown() {
        setProperty("lokal.database", Boolean.FALSE.toString());
        jetty.stop.run();
    }
}
