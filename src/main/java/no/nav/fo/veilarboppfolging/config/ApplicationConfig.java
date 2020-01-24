package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ServletUtil;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.common.auth.SecurityLevel;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.security.SecurityTokenServiceOidcProvider;
import no.nav.fo.veilarboppfolging.security.SecurityTokenServiceOidcProviderConfig;
import no.nav.internal.PopulerOppfolgingHistorikkServlet;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static no.nav.brukerdialog.security.Constants.AZUREADB2C_OIDC_COOKIE_NAME_FSS;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.migrateDatabase;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.resolveFromEnvironment;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Configuration
@EnableScheduling
@ComponentScan(
        basePackages = "no.nav.fo.veilarboppfolging",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test")}
)
@Import(AktorConfig.class)
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarboppfolging";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String AAD_B2C_CLIENTID_USERNAME_PROPERTY = "AAD_B2C_CLIENTID_USERNAME";
    public static final String AAD_B2C_CLIENTID_PASSWORD_PROPERTY = "AAD_B2C_CLIENTID_PASSWORD";
    public static final String VEILARBAKTIVITETAPI_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String AKTIVITETSPLAN_URL_PROPERTY = "AKTIVITETSPLAN_URL";
    public static final String VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY = "VIRKSOMHET_YTELSESKONTRAKT_V3_ENDPOINTURL";
    public static final String VIRKSOMHET_OPPFOLGING_V1_PROPERTY = "VIRKSOMHET_OPPFOLGING_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_OPPFOELGINGSSTATUS_V2_PROPERTY = "VIRKSOMHET_OPPFOELGINGSSTATUS_V2_ENDPOINTURL";
    public static final String VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY = "VIRKSOMHET_ORGANISASJONENHET_V2_ENDPOINTURL";
    public static final String VARSELOPPGAVE_V1_PROPERTY = "VARSELOPPGAVE_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY = "VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ENDPOINTURL";
    public static final String KAFKA_BROKERS_PROPERTY = "KAFKA_BROKERS_URL";
    public static final String OPPFOLGING_FEED_BRUKERTILGANG_PROPERTY = "oppfolging.feed.brukertilgang";
    public static final String AVSLUTTETOPPFOLGING_FEED_BRUKERTILGANG_PROPERTY = "avsluttetoppfolging.feed.brukertilgang";
    public static final String KVP_FEED_BRUKERTILGANG_PROPERTY = "kvp.feed.brukertilgang";
    public static final String NYEBRUKERE_FEED_BRUKERTILGANG_PROPERTY = "nyebrukere.feed.brukertilgang";
    public static final String KVP_API_BRUKERTILGANG_PROPERTY = "kvp.api.brukertilgang";
    public static final String VEILARBARENAAPI_URL_PROPERTY = "VEILARBARENAAPI_URL";
    public static final String STS_OIDC_CONFIGURATION_URL_PROPERTY = "SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL";
    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    public static final String APP_ENVIRONMENT_NAME = "APP_ENVIRONMENT_NAME";


    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;

    @Inject
    public SystemUserTokenProvider systemUserTokenProvider;

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(resolveFromEnvironment());
    }

    @Bean
    public Executor taskScheduler() {
        return Executors.newScheduledThreadPool(5);
    }

    @Override
    public void startup(ServletContext servletContext) {
        setProperty(OPPFOLGING_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje,srvpam-cv-api", PUBLIC);
        setProperty(AVSLUTTETOPPFOLGING_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet,srvveilarbjobbsoke", PUBLIC);
        setProperty(KVP_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet", PUBLIC);
        setProperty(NYEBRUKERE_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdirigent", PUBLIC);
        setProperty(KVP_API_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet", PUBLIC);
        jdbcTemplate.update("UPDATE \"schema_version\" SET \"checksum\"=-788301912 WHERE \"version\" = '1.16'");
        migrateDatabase(dataSource);

        ServletUtil.leggTilServlet(servletContext, new PopulerOppfolgingHistorikkServlet(oppfolgingsenhetHistorikkRepository, systemUserTokenProvider), "/internal/populer_enhet_historikk");
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        String discoveryUrl = getRequiredProperty("AAD_DISCOVERY_URL");
        String clientId = getRequiredProperty("VEILARBLOGIN_AAD_CLIENT_ID");

        AzureADB2CConfig config = AzureADB2CConfig.builder()
                .discoveryUrl(discoveryUrl)
                .expectedAudience(clientId)
                .identType(IdentType.InternBruker)
                .tokenName(AZUREADB2C_OIDC_COOKIE_NAME_FSS)
                .build();

        SecurityTokenServiceOidcProvider securityTokenServiceOidcProvider = new SecurityTokenServiceOidcProvider(SecurityTokenServiceOidcProviderConfig.builder()
                .discoveryUrl(getRequiredProperty(STS_OIDC_CONFIGURATION_URL_PROPERTY))
                .build());

        apiAppConfigurator
                .sts()
                .validateAzureAdExternalUserTokens(SecurityLevel.Level4)
                .validateAzureAdInternalUsersTokens(config)
                .customSecurityLevelForExternalUsers(SecurityLevel.Level3, "niva3")
                .issoLogin()
                .oidcProvider(securityTokenServiceOidcProvider);
    }
}
