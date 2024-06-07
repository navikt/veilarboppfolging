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
import no.nav.common.cxf.StsConfig;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.metrics.SensuConfig;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.poao_tilgang.client.AdGruppe;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient;
import no.nav.poao_tilgang.client.PolicyInput;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static no.nav.common.utils.NaisUtils.getCredentials;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties({EnvironmentProperties.class})
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

    @Bean
    public Credentials serviceUserCredentials() {
        return getCredentials("service_user");
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient(SensuConfig.defaultConfig());
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

    /*
     TODO brukes STS av noen lenger?
      - bruker i batch/kafka consumer for å sette authcontext
      @see no.nav.veilarboppfolging.service.IservService.finnBrukereOgAvslutt
      @see no.nav.veilarboppfolging.service.KafkaConsumerService.consumeEndringPaOppfolgingBruker

      Kan vi bruker en azureMachineTokenProvider som en drop-in erstatning? Må vi i så fall legge til veilarboppfolging i inbound access policy?
     */
    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getNaisStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient();
    }
    @Bean
    public static StsConfig stsConfig(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return StsConfig.builder()
                .url(properties.getSoapStsUrl())
                .username(serviceUserCredentials.username)
                .password(serviceUserCredentials.password)
                .build();
    }

    @Bean
    AuditLogger auditLogger() {
        return new AuditLoggerImpl();
    }

	@Bean
	public PoaoTilgangClient poaoTilgangClient(EnvironmentProperties properties, AzureAdMachineToMachineTokenClient tokenClient) {
		return new PoaoTilgangCachedClient(
				new PoaoTilgangHttpClient(
						properties.getPoaoTilgangUrl(),
						() -> tokenClient.createMachineToMachineToken(properties.getPoaoTilgangScope()),
						RestClient.baseClient()
				),
				policyInputToDecisionCache,
				navAnsattIdToAzureAdGrupperCache,
				norskIdentToErSkjermetCache
		);
	}

}
