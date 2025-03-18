package no.nav.veilarboppfolging.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.audit_log.log.AuditLoggerImpl;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.rest.client.RestClient;
import no.nav.poao_tilgang.api.dto.response.TilgangsattributterResponse;
import no.nav.poao_tilgang.client.AdGruppe;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient;
import no.nav.poao_tilgang.client.PolicyInput;
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties({EnvironmentProperties.class})
@Profile("!test")
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarboppfolging";

    public static final String SYSTEM_USER_NAME = "System";

	private final Cache<PolicyInput, Decision> policyInputToDecisionCache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(30))
			.build();
	private final Cache<UUID, List<AdGruppe>> navAnsattIdToAzureAdGrupperCache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(30))
			.build();
	private final Cache<String, Boolean> norskIdentToErSkjermetCache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(30))
			.build();
    private final Cache<String, TilgangsattributterResponse> tilgangsAttributterCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClient() {
            @Override
            public void report(Event event) {
                //TODO: Fiks
            }

            @Override
            public void report(String eventName, Map<String, Object> fields, Map<String, String> tags, long timestampInMilliseconds) {
                //TODO: Fiks
            }
        };
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient(LockProvider lockProvider) {
        return new ShedLockLeaderElectionClient(lockProvider);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public ErrorMappedAzureAdMachineToMachineTokenClient errorMappedAzureAdMachineToMachineTokenClient() {
        return new ErrorMappedAzureAdMachineToMachineTokenClient();
    }

    @Bean
    AuditLogger auditLogger() {
        return new AuditLoggerImpl();
    }

	@Bean
	public PoaoTilgangClient poaoTilgangClient(EnvironmentProperties properties, ErrorMappedAzureAdMachineToMachineTokenClient tokenClient) {
		return new PoaoTilgangCachedClient(
				new PoaoTilgangHttpClient(
						properties.getPoaoTilgangUrl(),
						() -> tokenClient.createMachineToMachineToken(properties.getPoaoTilgangScope()),
						RestClient.baseClient()
				),
				policyInputToDecisionCache,
				navAnsattIdToAzureAdGrupperCache,
				norskIdentToErSkjermetCache,
                tilgangsAttributterCache
		);
	}

}
