package no.nav.veilarboppfolging.config;

import no.nav.common.abac.Pep;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.feed.FeedConfig;
import no.nav.veilarboppfolging.kafka.KafkaHelsesjekk;
import no.nav.veilarboppfolging.kafka.KafkaTopics;
import no.nav.veilarboppfolging.mock.PepMock;
import no.nav.veilarboppfolging.mock.UnleashServiceMock;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        SwaggerConfig.class,
        ClientTestConfig.class,
        ControllerTestConfig.class,
        RepositoryTestConfig.class,
        ServiceTestConfig.class,
        FilterTestConfig.class,
        KafkaTestConfig.class,
        FeedConfig.class,
        HelsesjekkConfig.class
})
public class ApplicationTestConfig {

    @Bean
    public SystemUserTokenProvider openAmSystemUserTokenProvider() {
        return () -> "OPEN_AM_SYSTEM_USER_TOKEN";
    }

    @Bean
    public UnleashService unleashService() {
        return UnleashServiceMock.getMock();
    }

    @Bean
    public KafkaTopics kafkaTopics() {
        return KafkaTopics.create("local");
    }

    @Bean
    public KafkaHelsesjekk kafkaHelsesjekk() {
        return new KafkaHelsesjekk();
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock();
    }

    @Bean
    public DataSource dataSource() {
        return LocalH2Database.getDb().getDataSource();
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }


}
