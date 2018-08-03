package no.nav.fo.veilarboppfolging;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;

import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.configureDataSource;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


class StartJetty {

    private static final String APPLICATION_NAME = "veilarboppfolging";

    static final String CONTEXT_NAME = "/veilarboppfolging";
    static final int PORT = 8587;

    public static void main(String[] args) throws Exception {

        loadTestConfigFromProperties();
        
        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .addDatasource(configureDataSource(APPLICATION_NAME), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static void loadTestConfigFromProperties() {
        try {
            SystemProperties.setFrom("environment-local.properties");
        } catch (Exception e) {
            SystemProperties.setFrom("environment-test.properties");
        }
    }

}
