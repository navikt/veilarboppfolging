package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class DbTestUtils {

    private final static List<String> ALL_TABLES = Arrays.asList(
            "OPPFOLGINGSPERIODE",
            "OPPFOLGINGSTATUS",
            "KVP",
            "UTMELDING",
            "OPPFOLGINGSENHET_ENDRET"
    );

    public static void setupDatabaseFunctions(DataSource dataSource) {
        runScript(dataSource, "oracle-mock.sql");
    }

    public static void cleanupTestDb() {
        cleanupTestDb(LocalH2Database.getDb());
    }

    public static void cleanupTestDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

    public static TransactionTemplate createTransactor(JdbcTemplate db) {
        return new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
    }

    public static DataSource createTestDataSource(String dbUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(TestDriver.class.getName());
        dataSource.setUrl(dbUrl);
        return dataSource;
    }

    public static void initDb(DataSource dataSource) {
        var flyway = new Flyway(Flyway.configure()
                .dataSource(dataSource)
                .table("schema_version")
                .validateMigrationNaming(true));
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

    @SneakyThrows
    public static void runScript(DataSource dataSource, String resourceFile) {
        try(Statement statement = dataSource.getConnection().createStatement()) {
            String sql = TestUtils.readTestResourceFile(resourceFile);
            statement.execute(sql);
        }
    }
}
