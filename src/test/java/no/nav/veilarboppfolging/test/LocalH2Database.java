package no.nav.veilarboppfolging.test;

import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.veilarboppfolging.test.DbTestUtils.initDb;
import static no.nav.veilarboppfolging.test.DbTestUtils.runScript;

public class LocalH2Database {

    private static volatile JdbcTemplate db;
    private static final AtomicBoolean useInnMemory = new AtomicBoolean(true);

    public static void setUsePersistentDb() {
        useInnMemory.set(false);
    }

    public static JdbcTemplate getDb() {
        if (useInnMemory.get()) {
            return getDb("jdbc:h2:mem:veilarboppfolging-local;DB_CLOSE_DELAY=-1;MODE=Oracle;BUILTIN_ALIAS_OVERRIDE=1;");
        }
        return getDb("jdbc:h2:file:~/database/veilarboppfolging;DB_CLOSE_DELAY=-1;MODE=Oracle;AUTO_SERVER=TRUE;BUILTIN_ALIAS_OVERRIDE=1;");
    }

    public static synchronized JdbcTemplate getDb(String dbUrl) {
        if (db == null) {
            TestDriver.init();

            DataSource testDataSource = DbTestUtils.createTestDataSource(dbUrl);
            db = new JdbcTemplate(testDataSource);
            initDb(testDataSource);
        }

        return db;
    }

}
