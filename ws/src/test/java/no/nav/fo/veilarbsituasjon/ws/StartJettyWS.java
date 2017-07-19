package no.nav.fo.veilarbsituasjon.ws;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJettyWS {

    private static final String APPLICATION_NAME = "veilarbsituasjon";
    private static final TestEnvironment TEST_ENVIRONMENT = TestEnvironment.T6;

    private static final int PORT = 8382;

    public static void main(String[] args) throws Exception {
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            setupInMemoryDatabase();
        } else {
            setupJndiLocalContext(FasitUtils.getDbCredentials(TEST_ENVIRONMENT,APPLICATION_NAME));
        }

        Jetty jetty = DevelopmentSecurity.setupSamlLogin(usingWar()
                        .at("/veilarbsituasjon-ws")
                        .loadProperties("/environment-test.properties")
                        .port(PORT)
                        .sslPort(PORT + 1)
                        .overrideWebXml()
                , new DevelopmentSecurity.SamlSecurityConfig(APPLICATION_NAME, TEST_ENVIRONMENT.toString())
        ).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
