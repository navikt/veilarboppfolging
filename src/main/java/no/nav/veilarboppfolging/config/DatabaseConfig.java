package no.nav.veilarboppfolging.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.jdbc.DataSourceFactory;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.jdbc.Transactor;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;


import javax.sql.DataSource;
import java.util.UUID;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    public static final String VEILARBOPPFOLGINGDB_URL_PROPERTY = "VEILARBOPPFOLGINGDB_URL";
    public static final String VEILARBOPPFOLGINGDB_USERNAME_PROPERTY = "VEILARBOPPFOLGINGDB_USERNAME";
    public static final String VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY = "VEILARBOPPFOLGINGDB_PASSWORD";

    @Bean
    public DataSource dataSource() {
        return DataSourceFactory.dataSource()
                .url(getRequiredProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY))
                .username(getRequiredProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY))
                .password(getOptionalProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY).orElse(""))
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource datasource) {
        return new JdbcTemplate(datasource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public Database database(JdbcTemplate jdbcTemplates) {
        return new Database(jdbcTemplates);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager platformTransactionManager) {
        return new Transactor(platformTransactionManager);
    }

    @Bean
    public Pingable dbPinger(final DataSource ds) {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "veilarboppfolgingDB: " + getRequiredProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY),
                "Enkel spørring mot Databasen for VeilArbOppfolging.",
                true
        );
        return () -> {
            try {
                jdbcTemplate(ds).queryForObject("SELECT 1 FROM DUAL", Long.class);
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

    @Bean
    public LockingTaskExecutor taskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }

    public static void migrateDatabase(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }
}
