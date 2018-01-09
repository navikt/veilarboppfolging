package no.nav.fo.veilarboppfolging.config;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.fo.veilarboppfolging.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class JndiLocalContextConfig {

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