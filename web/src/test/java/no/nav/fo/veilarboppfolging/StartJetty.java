package no.nav.fo.veilarboppfolging;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.fo.veilarboppfolging.config.SecurityTestConfig.setupLdap;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

class StartJetty {

    private static final String APPLICATION_NAME = "veilarboppfolging";

    static final String CONTEXT_NAME = "/veilarboppfolging";
    static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        boolean lokalDatabase = Boolean.parseBoolean(getProperty("lokal.database"));

        setupLdap();

        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .loadProperties("/environment-test.properties")
                        .addDatasource(lokalDatabase ? setupInMemoryDatabase() : setupJndiLocalContext(getDbCredentials(APPLICATION_NAME)), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
