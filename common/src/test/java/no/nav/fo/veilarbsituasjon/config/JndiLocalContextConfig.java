package no.nav.fo.veilarbsituasjon.config;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.fo.veilarbsituasjon.db.testdriver.TestDriver;
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
        return ds;
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.URL);
        ds.setUsername("sa");
        ds.setPassword("");

        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbsituasjonDB");
        flyway.setDataSource(ds);
        flyway.setRepeatableSqlMigrationPrefix("N/A");

        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));

        return ds;
    }
}