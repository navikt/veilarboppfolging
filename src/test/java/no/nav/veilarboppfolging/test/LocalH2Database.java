package no.nav.veilarboppfolging.test;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class LocalH2Database {

    private static JdbcTemplate db;

    public static JdbcTemplate getDb() {
        if (db == null) {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:veilarboppfolging-local;DB_CLOSE_DELAY=-1;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=3;");

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
