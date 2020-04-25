package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.util.StringUtils;
import no.nav.fasit.DbCredentials;
import no.nav.fo.veilarboppfolging.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static java.lang.System.getProperty;
import static no.nav.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
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
        return (StringUtils.notNullOrEmpty(dbUrl)) ?
                new DbCredentials().setUrl(dbUrl).setUsername(dbUser).setPassword(dbPasswd) :
                getDbCredentials(applicationName);
    }

    public static AbstractDataSource setupJndiLocalContext(DbCredentials dbCredentials) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(dbCredentials.url);
        ds.setUsername(dbCredentials.username);
        ds.setPassword(dbCredentials.password);
        if (dbCredentials.url.contains("h2")) {
            ds.setDriverClassName(TestDriver.class.getName());
            migrerDatabase(ds);
        }
        return ds;
    }

    public static AbstractDataSource setupInMemoryDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.createInMemoryDatabaseUrl());
        ds.setUsername("sa");
        ds.setPassword("");

        int antallMigreringer = migrerDatabase(ds);
        assertThat(antallMigreringer, greaterThan(0));

        return ds;
    }

    private static int migrerDatabase(AbstractDataSource ds) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        return flyway.migrate();
    }
}
