package no.nav.veilarboppfolging.test;

import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class DbTestUtils {

    private final static List<String> ALL_TABLES = Arrays.asList(
            "OPPFOLGINGSPERIODE",
            "OPPFOLGINGSTATUS",
            "ESKALERINGSVARSEL",
            "KVP",
            "UTMELDING",
            "OPPFOLGINGSENHET_ENDRET"
    );

    public static void cleanupTestDb() {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(LocalH2Database.getDb(), table));
    }

    public static TransactionTemplate getTransactor(JdbcTemplate db) {
        return new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
    }

    public static DataSource createTestDataSource(String dbUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(TestDriver.class.getName());
        dataSource.setUrl(dbUrl);
        return dataSource;
    }

    public static void initDb(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

}