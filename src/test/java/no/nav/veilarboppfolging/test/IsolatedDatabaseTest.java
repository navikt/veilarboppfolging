package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.test.DbTestUtils.initDb;

/**
 * Creates and shuts down a new database for each test
 */
public abstract class IsolatedDatabaseTest {

    private static final AtomicInteger databaseCounter = new AtomicInteger();

    protected JdbcTemplate db;

    @Before
    public void setupIsolatedDatabase() {
        String dbUrl = format("jdbc:h2:mem:veilarboppfolging-local-%d;DB_CLOSE_DELAY=-1;MODE=Oracle;", databaseCounter.incrementAndGet());
        DataSource testDataSource = DbTestUtils.createTestDataSource(dbUrl);

        initDb(testDataSource);

        db = new JdbcTemplate(testDataSource);
    }

    @After
    @SneakyThrows
    public void shutdownIsolatedDatabase() {
        Connection connection = db.getDataSource().getConnection();
        connection.createStatement().execute("SHUTDOWN");
        connection.close();
    }

}
