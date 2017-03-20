package no.nav.fo.veilarbsituasjon.ws;

import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.jaas.JAASLoginService;

import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.MessageQueueMockConfig.setupBrokerService;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;

public class StartJettyWS {

    public static void main(String[] args) throws Exception {
        jettyBuilder(8383).buildJetty().start();
    }

    public static Jetty.JettyBuilder jettyBuilder(int port) throws Exception {
        setupInMemoryDatabase();
        setupBrokerService();

        return usingWar()
                .at("veilarbsituasjon-ws")
                .port(port)
                .sslPort(port + 1)
                .loadProperties("/environment-test.properties")
                .withLoginService(createSAMLLoginService())
                .overrideWebXml();
    }

    private static JAASLoginService createSAMLLoginService() {
        System.setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", StaticSubjectHandler.class.getName());
        System.setProperty("java.security.auth.login.config", StartJettyWS.class.getResource("/login.conf").toExternalForm());
        JAASLoginService jaasLoginService = new JAASLoginService("SAML Realm");
        jaasLoginService.setLoginModuleName("saml");
        return jaasLoginService;
    }

}
