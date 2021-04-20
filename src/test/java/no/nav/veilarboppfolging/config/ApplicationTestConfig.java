package no.nav.veilarboppfolging.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.sts.OpenAmSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.feed.FeedConfig;
import no.nav.veilarboppfolging.mock.PepMock;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public OpenAmSystemUserTokenProvider openAmSystemUserTokenProvider() {
        OpenAmSystemUserTokenProvider mockProvider = mock(OpenAmSystemUserTokenProvider.class);
        when(mockProvider.getSystemUserToken()).thenReturn("OPEN_AM_SYSTEM_USER_TOKEN");
        return mockProvider;
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        return () -> new PlainJWT(new JWTClaimsSet.Builder().build()).serialize();
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
        DataSource dataSource = LocalH2Database.getDb().getDataSource();
        DbTestUtils.setupDatabaseFunctions(dataSource);
        return dataSource;
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return () -> true;
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }
}
