package no.nav.veilarboppfolging.test;

import no.nav.veilarboppfolging.LocalDatabaseSingleton;
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
    private final DataSource dataSource = LocalDatabaseSingleton.INSTANCE.getPostgres();
    protected JdbcTemplate db = new JdbcTemplate(dataSource);
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    protected TransactionTemplate transactor = DbTestUtils.createTransactor(db);

    @Before
    public void cleanUp() {
        DbTestUtils.cleanupTestDb();
    }
}
