package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.fo.veilarboppfolging.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class JndiLocalContextConfig {

    public static DataSource configureDataSource(String applicationName) {
        return Boolean.parseBoolean(getProperty("lokal.database", "true")) ? setupInMemoryDatabase() :
            setupJndiLocalContext(configureCredentials(applicationName));
    }
 
    private static DbCredentials configureCredentials(String applicationName) {
        String dbUrl = getProperty("database.url");
        String dbUser = getProperty("database.user", "sa");
        String dbPasswd = getProperty("database.password", "");
        return(StringUtils.notNullOrEmpty(dbUrl)) ? 
            new DbCredentials().setUrl(dbUrl).setUsername(dbUser).setPassword(dbPasswd) :
                getDbCredentials(applicationName);
    }
    
    public static SingleConnectionDataSource setupJndiLocalContext(DbCredentials dbCredentials) {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl(dbCredentials.url);
        ds.setUsername(dbCredentials.username);
        ds.setPassword(dbCredentials.password);
        ds.setSuppressClose(true);
        if(dbCredentials.url.contains("h2")) {
            ds.setDriverClassName(TestDriver.class.getName());
            migrerDatabase(ds);
        }
        return ds;
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.createInMemoryDatabaseUrl());
        ds.setUsername("sa");
        ds.setPassword("");

        int antallMigreringer = migrerDatabase(ds);
        assertThat(antallMigreringer, greaterThan(0));

        return ds;
    }

    private static int migrerDatabase(SingleConnectionDataSource ds) {
        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarboppfolgingDB");
        flyway.setDataSource(ds);
        flyway.setRepeatableSqlMigrationPrefix("N/A");

        int migrate = flyway.migrate();
        return migrate;
    }
}