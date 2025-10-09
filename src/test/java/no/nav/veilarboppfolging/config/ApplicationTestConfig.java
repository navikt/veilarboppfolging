package no.nav.veilarboppfolging.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.audit_log.cef.CefMessage;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient;
import no.nav.veilarboppfolging.client.norg.INorgTilhorighetClient;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.io.IOException;

import static org.mockito.Mockito.mock;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationTestConfig {
    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return mock(AzureAdOnBehalfOfTokenClient.class);
    }

    @Bean
    public BigQueryClient bigQueryClient() {
        return mock(BigQueryClient.class);
    }

    @Bean
    public DataSource dataSource() throws IOException {
        var db = EmbeddedPostgres.start().getPostgresDatabase();
        DbTestUtils.initDb(db);
        return db;
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
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return () -> true;
    }

    @Bean
    public AuditLogger auditLogger() {
        return new AuditLogger() {
            @Override
            public void log(CefMessage message) {
                return;
            }

            @Override
            public void log(String message) {
                return;
            }
        };
    }

	@Bean
	public PoaoTilgangClient poaoTilgangClient() { return mock(PoaoTilgangClient.class); }

    @Bean
    public AmtDeltakerClient amtDeltakerClient() {
        return mock(AmtDeltakerClient.class);
    }

    @Bean
    public INorgTilhorighetClient inorgTilhorighetClient() {
        return mock(INorgTilhorighetClient.class);
    }
}
