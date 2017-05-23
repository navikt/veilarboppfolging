package no.nav.fo.veilarbsituasjon;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.fo.veilarbsituasjon.config.MessageQueueMockConfig.setupBrokerService;
import static no.nav.fo.veilarbsituasjon.config.SecurityTestConfig.setupLdap;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

class StartJetty {

    static final String CONTEXT_NAME = "/veilarbsituasjon";
    static final int PORT = 8486;
    private static final int SSL_PORT = 8485;

    public static void main(String[] args) throws Exception {
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            setupInMemoryDatabase();
        } else {
            setupJndiLocalContext();
        }

        setupLdap();
        setupBrokerService();

        Jetty jetty = setupISSO(usingWar()
                .at(CONTEXT_NAME)
                .sslPort(SSL_PORT)
                .port(PORT)
                .loadProperties("/environment-test.properties")
                .overrideWebXml()
                ,  new ISSOSecurityConfig("veilarbsituasjon","t6")).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
