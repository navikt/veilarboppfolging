package no.nav.fo.veilarbsituasjon.ws;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.fo.veilarbsituasjon.config.MessageQueueMockConfig.setupBrokerService;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJettyWS {

    private static final int PORT = 8382;

    public static void main(String[] args) throws Exception {
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            setupInMemoryDatabase();
        } else {
            setupJndiLocalContext();
        }

        setupBrokerService();

        Jetty jetty = DevelopmentSecurity.setupSamlLogin(usingWar()
                        .at("/veilarbsituasjon-ws")
                        .loadProperties("/environment-test.properties")
                        .port(PORT)
                        .sslPort(PORT + 1)
                        .overrideWebXml()
                , new DevelopmentSecurity.SamlSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
