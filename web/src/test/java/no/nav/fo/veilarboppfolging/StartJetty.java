package no.nav.fo.veilarboppfolging;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

import javax.sql.DataSource;

class StartJetty {

    private static final String APPLICATION_NAME = "veilarboppfolging";

    static final String CONTEXT_NAME = "/veilarboppfolging";
    static final int PORT = 8587;

    public static void main(String[] args) throws Exception {

        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .loadProperties("/environment-test.properties")
                        .addDatasource(configureDataSource(), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static DataSource configureDataSource() {
        return Boolean.parseBoolean(getProperty("lokal.database", "true")) ? setupInMemoryDatabase() :
            setupJndiLocalContext(configureCredentials());
    }
 
    private static DbCredentials configureCredentials() {
        String dbUrl = getProperty("database.url");
        String dbUser = getProperty("database.user", "sa");
        String dbPasswd = getProperty("database.password", "");
        return(StringUtils.notNullOrEmpty(dbUrl)) ? 
            new DbCredentials().setUrl(dbUrl).setUsername(dbUser).setPassword(dbPasswd) :
                getDbCredentials(APPLICATION_NAME);
    }

}
