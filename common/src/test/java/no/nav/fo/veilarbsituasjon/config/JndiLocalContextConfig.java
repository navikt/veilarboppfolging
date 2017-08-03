package no.nav.fo.veilarbsituasjon.config;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.fo.veilarbsituasjon.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class JndiLocalContextConfig {

    public static void setupJndiLocalContext(DbCredentials dbCredentials) {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl(dbCredentials.url);
        ds.setUsername(dbCredentials.username);
        ds.setPassword(dbCredentials.password);
        ds.setSuppressClose(true);
        registerJndi(ds);
    }

    private static void registerJndi(SingleConnectionDataSource ds) {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        try {
            InitialContext ctx = new InitialContext();
            ctx.createSubcontext("java:/");
            ctx.createSubcontext("java:/jboss/");
            ctx.createSubcontext("java:/jboss/jdbc/");

            ctx.bind("java:/jboss/jdbc/veilarbsituasjonDS", ds);

        } catch (NamingException e) {
            System.out.printf(e.toString());
        }
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.URL);
        ds.setUsername("sa");
        ds.setPassword("");

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbsituasjonDB");
        flyway.setDataSource(ds);
        flyway.setRepeatableSqlMigrationPrefix("N/A");

        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));

        registerJndi(ds);

        return ds;
    }
}