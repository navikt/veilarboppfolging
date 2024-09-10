package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates and shuts down a new database for each test
 */
public abstract class IsolatedDatabaseTest {

    private static final AtomicInteger databaseCounter = new AtomicInteger();

    protected JdbcTemplate db;
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    protected TransactionTemplate transactor;

    @Before
    public void setupIsolatedDatabase() {
        final DataSource dataSource = LocalDatabaseSingleton.INSTANCE.getPostgres();
        db = new JdbcTemplate(dataSource);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(db);
        transactor = DbTestUtils.createTransactor(db);
    }

    @After
    @SneakyThrows
    public void shutdownIsolatedDatabase() {
//        Connection connection = db.getDataSource().getConnection();
//        connection.createStatement().execute("SHUTDOWN");
//        connection.close();
    }

}
