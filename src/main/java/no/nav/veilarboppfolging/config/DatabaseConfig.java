package no.nav.veilarboppfolging.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import static no.nav.veilarboppfolging.dbutil.DatabaseMigratorKt.migrateDb;

@Configuration
@EnableConfigurationProperties({DatabaseConfig.DatasourceProperties.class, DatabaseConfig.DatasourceFlywayProperties.class})
@RequiredArgsConstructor
public class DatabaseConfig {

    private final DatasourceProperties datasourceProperties;
    private final DatasourceFlywayProperties datasourceFlywayProperties;

    @Bean
    public DataSource dataSource() {
        runFlywayMigration();
        var config = new HikariConfig();
        config.setSchema("veilarboppfolging");
        config.setJdbcUrl(datasourceProperties.url);
        config.setUsername(datasourceProperties.username);
        config.setPassword(datasourceProperties.password);
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    private void runFlywayMigration() {
        var config = new HikariConfig();
        config.setSchema("veilarboppfolging");
        config.setJdbcUrl(datasourceFlywayProperties.url);
        config.setUsername(datasourceFlywayProperties.username);
        config.setPassword(datasourceFlywayProperties.password);
        config.setMaximumPoolSize(5);
        var db = new HikariDataSource(config);
        migrateDb(db);
    }

    @Bean
    public JdbcTemplate db(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.datasource")
    public static class DatasourceProperties {
        String url;
        String username;
        String password;
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.datasource.flyway")
    public static class DatasourceFlywayProperties {
        String url;
        String username;
        String password;
    }

}
