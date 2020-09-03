package no.nav.veilarboppfolging.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.common.utils.Credentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.common.utils.NaisUtils.getCredentials;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    private final EnvironmentProperties environmentProperties;

    private final Credentials oracleCredentials;

    public DatabaseConfig(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
        oracleCredentials = getCredentials("oracle_creds");
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environmentProperties.getDbUrl());
        config.setUsername(oracleCredentials.username);
        config.setPassword(oracleCredentials.password);
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate db(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
