package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DbTestUtils {

    private final static List<String> ALL_TABLES = Arrays.asList(
            "SAK",
            "OPPFOLGINGSPERIODE",
            "OPPFOLGINGSTATUS",
            "KVP",
            "UTMELDING",
            "OPPFOLGINGSENHET_ENDRET"
    );

    public static void cleanupTestDb() {
        cleanupTestDb(LocalDatabaseSingleton.INSTANCE.getJdbcTemplate());
    }

    public static void cleanupTestDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

    public static TransactionTemplate createTransactor(JdbcTemplate db) {
        return new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
    }

    public static void initDb(DataSource dataSource) {
        try(Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("""
              CREATE USER veilarboppfolging NOLOGIN;
              GRANT CONNECT on DATABASE postgres to veilarboppfolging;
              GRANT USAGE ON SCHEMA public to veilarboppfolging;
            """.trim()).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Properties properties = new Properties();
        properties.put("flyway.cleanDisabled", false);
        FluentConfiguration config = Flyway
                .configure()
                .dataSource(dataSource)
                .table("schema_version")
                .configuration(properties)
                .cleanOnValidationError(true)
                .validateMigrationNaming(true);
        Flyway flyway = new Flyway(config);
        flyway.clean();
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

    @SneakyThrows
    public static void runScript(DataSource dataSource, String resourceFile) {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            String sql = TestUtils.readTestResourceFile(resourceFile);
            statement.execute(sql);
        }
    }

    public static void lagreOppfølgingsperiode(OppfolgingsperiodeEntity periode) {
        LocalDatabaseSingleton.INSTANCE.getJdbcTemplate().update(
                "" +
                        "INSERT INTO OPPFOLGINGSPERIODE(uuid, aktor_id, startDato, oppdatert, start_begrunnelse) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)",
                periode.getUuid().toString(), periode.getAktorId(), periode.getStartDato(), periode.getStartetBegrunnelse().name()
        );
    }
}
