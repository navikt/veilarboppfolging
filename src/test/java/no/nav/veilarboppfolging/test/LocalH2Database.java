package no.nav.veilarboppfolging.test;

import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.veilarboppfolging.test.DbTestUtils.initDb;

public class LocalH2Database {

    private static volatile JdbcTemplate db;

    public static synchronized JdbcTemplate getDb() {
        if (db == null) {
            TestDriver.init();

            DataSource testDataSource = DbTestUtils.createTestDataSource("jdbc:h2:mem:veilarboppfolging-local;DB_CLOSE_DELAY=-1;MODE=Oracle;");
            db = new JdbcTemplate(testDataSource);
            initDb(testDataSource);
        }

        return db;
    }

}
