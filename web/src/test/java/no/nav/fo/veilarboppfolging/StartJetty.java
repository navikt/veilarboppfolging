package no.nav.fo.veilarboppfolging;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.configureDataSource;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

class StartJetty {

    private static final String APPLICATION_NAME = "veilarboppfolging";

    static final String CONTEXT_NAME = "/veilarboppfolging";
    static final int PORT = 8587;

    public static void main(String[] args) throws Exception {

        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .loadProperties("/environment-test.properties")
                        .addDatasource(configureDataSource(APPLICATION_NAME), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();

        setupTestContext();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
