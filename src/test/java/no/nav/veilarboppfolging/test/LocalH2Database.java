package no.nav.veilarboppfolging.test;

import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

public class LocalH2Database {

    private static JdbcTemplate db;

    public static JdbcTemplate getDb() {
        if (db == null) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(TestDriver.class.getName());
            dataSource.setUrl("jdbc:h2:mem:veilarboppfolging-local;DB_CLOSE_DELAY=-1;MODE=Oracle;"); // Add this for debug info: "TRACE_LEVEL_SYSTEM_OUT=3;"

            db = new JdbcTemplate(dataSource);
            initDb(db);
        }

        return db;
    }

    public static TransactionTemplate getTransactor() {
        return new TransactionTemplate(new DataSourceTransactionManager(getDb().getDataSource()));
    }

    private static void initDb(JdbcTemplate db) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(db.getDataSource());
        flyway.migrate();
    }

}
