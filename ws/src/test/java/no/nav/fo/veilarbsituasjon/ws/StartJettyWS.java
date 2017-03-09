package no.nav.fo.veilarbsituasjon.ws;

import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.fo.veilarbsituasjon.StartJetty.setupBrokerService;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;

public class StartJettyWS {

    public static void main(String[] args) throws Exception {
        startJetty(8081).start();
    }

    public static Jetty startJetty(int port) throws Exception {
//        System.setProperty(SubjectHandler.SUBJECTHANDLER_KEY, JettySubjectHandler.class.getName());
//        System.setProperty("java.security.auth.login.config", StartJettyWS.class.getResource("/login.conf").toExternalForm());
//
//        JettySubjectHandler jettySubjectHandler = null;
//        no.nav.brukerdialog.security.context.SubjectHandler subjectHandler = jettySubjectHandler;

//        JAASLoginService jaasLoginService = new JAASLoginService("SAML Realm");
//        jaasLoginService.setLoginModuleName("saml");

        setupInMemoryDatabase();
        setupBrokerService();

        return usingWar()
                .at("veilarbsituasjon-ws")
                .port(port)
                .loadProperties("/environment-test.properties")
//                .withLoginService(jaasLoginService)
                .overrideWebXml()
                .buildJetty();
    }

}
