package no.nav.fo.veilarboppfolging.ws;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJettyWS {

    private static final String APPLICATION_NAME = "veilarboppfolging";
    private static final TestEnvironment TEST_ENVIRONMENT = TestEnvironment.T6;

    private static final int PORT = 8688;

    public static void main(String[] args) throws Exception {
        boolean lokalDatabase = Boolean.parseBoolean(getProperty("lokal.database"));
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(usingWar()
                        .at("/veilarboppfolging-ws")
                        .loadProperties("/environment-test.properties")
                        .port(PORT)
                        .sslPort(PORT + 1)
                        .addDatasource(lokalDatabase ? setupInMemoryDatabase() : setupJndiLocalContext(getDbCredentials(TEST_ENVIRONMENT, APPLICATION_NAME)), DATA_SOURCE_JDNI_NAME)
                , new DevelopmentSecurity.SamlSecurityConfig(APPLICATION_NAME)
        ).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
