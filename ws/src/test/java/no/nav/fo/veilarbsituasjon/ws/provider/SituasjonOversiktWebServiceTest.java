package no.nav.fo.veilarbsituasjon.ws.provider;

import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.BehandleSituasjonV1;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.HentVilkaarSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarRequest;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.HentVilkaarResponse;
import org.junit.jupiter.api.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbsituasjon.ws.StartJettyWS.jettyBuilder;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public class SituasjonOversiktWebServiceTest {

    private static Jetty jetty;
    private BehandleSituasjonV1 behandleSituasjonV1;

    @BeforeAll
    public static void setUp() throws Exception {
//        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
//        setProperty("lokal.database", Boolean.TRUE.toString());
        jetty = jettyBuilder(32548).loadProperties("/situasjonOversiktWebServiceTest.properties").buildJetty();
        jetty.start();
    }

    @BeforeEach
    public void before() {
//        SslContextFactory sslContextFactory = new SslContextFactory();
//        sslContextFactory.setTrustAll(true);
//
//        HttpClient httpClient = new HttpClient(sslContextFactory);
//        httpClient.start();
//        ContentResponse contentResponse = httpClient
//                .newRequest("https://itjenester-t6.oera.no/esso/identity/authenticate")
//                .param("username", "***REMOVED***")
//                .param("password", "Eifel123")
//                .send();
//        String openAMResponse = "";
        String sso = "AQIC5wM2LY4SfczLOGUfQftGIJKYsDku3SzQeL76Jg69NBs.*AAJTSQACMDIAAlMxAAIwMQ..*";

        StaticSubjectHandler subjectHandler = (StaticSubjectHandler) SubjectHandler.getSubjectHandler();
        Subject subject = subjectHandler.getSubject();
        Set<Principal> principals = subject.getPrincipals();
        principals.clear();
        principals.add(SluttBruker.eksternBruker("***REMOVED***"));
        principals.add(new ConsumerId("***REMOVED***"));
        Set<Object> publicCredentials = subject.getPublicCredentials();
        publicCredentials.clear();
        publicCredentials.add(new OpenAmTokenCredential(sso));
        publicCredentials.add(new AuthenticationLevelCredential(4));


        behandleSituasjonV1 = new CXFClient<>(BehandleSituasjonV1.class)
                .address("https://localhost:32549/veilarbsituasjon-ws/ws/Situasjon")
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

    @AfterAll
    public static void tearDown() {
        setProperty("lokal.database", Boolean.FALSE.toString());
        jetty.stop.run();
    }
}
