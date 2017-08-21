package no.nav.fo.veilarbsituasjon;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarbsituasjon.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.fo.veilarbsituasjon.config.SecurityTestConfig.setupLdap;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

class StartJetty {

    private static final String APPLICATION_NAME = "veilarbsituasjon";
    private static final TestEnvironment TEST_ENVIRONMENT = TestEnvironment.T6;

    static final String CONTEXT_NAME = "/veilarbsituasjon";
    static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        boolean lokalDatabase = Boolean.parseBoolean(getProperty("lokal.database"));

        setupLdap();

        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .loadProperties("/environment-test.properties")
                        .addDatasource(lokalDatabase ? setupInMemoryDatabase() : setupJndiLocalContext(getDbCredentials(TEST_ENVIRONMENT, APPLICATION_NAME)), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME, TEST_ENVIRONMENT.toString())).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
