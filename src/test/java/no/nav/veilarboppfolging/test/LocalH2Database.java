package no.nav.veilarboppfolging.test;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.veilarboppfolging.test.DbTestUtils.initDb;

public class LocalH2Database {

    private static JdbcTemplate db;

    public static JdbcTemplate getDb() {
        if (db == null) {
            DataSource testDataSource = DbTestUtils.createTestDataSource("jdbc:h2:mem:veilarboppfolging-local;DB_CLOSE_DELAY=-1;MODE=Oracle;");
            db = new JdbcTemplate(testDataSource);
            initDb(db.getDataSource());
        }

        return db;
    }

}
