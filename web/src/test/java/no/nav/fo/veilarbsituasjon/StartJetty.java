package no.nav.fo.veilarbsituasjon;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.apache.activemq.broker.BrokerService;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;

import javax.security.auth.message.config.AuthConfigFactory;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;

class StartJetty {
    private static final int PORT = 8486;
    private static final int SSL_PORT = 8485;

    public static void main(String[] args) throws Exception {
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            setupInMemoryDatabase();
        } else {
            setupJndiLocalContext();
        }

        setupBrokerService();
        setupAutentisering();

        Jetty jetty = usingWar()
                .at("/veilarbsituasjon")
                .sslPort(SSL_PORT)
                .port(PORT)
                .loadProperties("/environment-test.properties")
                .overrideWebXml()
                .configureForJaspic()
                .buildJetty();
        jetty.start();
    }

    private static void setupBrokerService() throws Exception {
        final BrokerService broker = new BrokerService();
        broker.getSystemUsage().getTempUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.getSystemUsage().getStoreUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.addConnector("tcp://localhost:61616");
        broker.start();
    }

    private static void setupAutentisering() {
        SystemProperties.setFrom("environment-test.properties");
        System.setProperty("develop-local", "true");
        System.setProperty("org.apache.geronimo.jaspic.configurationFile", "web/src/test/resources/jaspiconf.xml");
        InternbrukerSubjectHandler.setVeilederIdent("Z990610");
        InternbrukerSubjectHandler.setServicebruker("srvveilarbveileder");
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
    }
}
