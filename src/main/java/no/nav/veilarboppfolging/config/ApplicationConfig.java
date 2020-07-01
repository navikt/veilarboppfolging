package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.cxf.StsConfig;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.metrics.SensuConfig;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.NaisUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static no.nav.common.featuretoggle.UnleashServiceConfig.resolveFromEnvironment;
import static no.nav.common.utils.NaisUtils.getCredentials;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarboppfolging";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String AAD_B2C_CLIENTID_USERNAME_PROPERTY = "AAD_B2C_CLIENTID_USERNAME";
    public static final String AAD_B2C_CLIENTID_PASSWORD_PROPERTY = "AAD_B2C_CLIENTID_PASSWORD";
    public static final String VEILARBAKTIVITETAPI_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String ARBEIDSRETTET_DIALOG_PROPERTY = "ARBEIDSRETTET_DIALOG_URL";
    public static final String VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY = "VIRKSOMHET_YTELSESKONTRAKT_V3_ENDPOINTURL";
    public static final String VIRKSOMHET_OPPFOLGING_V1_PROPERTY = "VIRKSOMHET_OPPFOLGING_V1_ENDPOINTURL";
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

    @Bean
    public Credentials serviceUserCredentials() {
        return getCredentials("service_user");
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient(SensuConfig.defaultConfig());
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
    public UnleashService unleashService() {
        return new UnleashService(resolveFromEnvironment());
    }

    @Bean
    public Executor taskScheduler() {
        return Executors.newScheduledThreadPool(5);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        Credentials serviceUserCredentials = NaisUtils.getCredentials("service_user");
        return new VeilarbPep(
                properties.getAbacUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

//    setProperty(OPPFOLGING_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje,srvpam-cv-api", PUBLIC);
//    setProperty(AVSLUTTETOPPFOLGING_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet,srvveilarbjobbsoke", PUBLIC);
//    setProperty(KVP_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet", PUBLIC);
//    setProperty(NYEBRUKERE_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbdirigent", PUBLIC);
//    setProperty(KVP_API_BRUKERTILGANG_PROPERTY, "srvveilarbdialog,srvveilarbaktivitet", PUBLIC);
//
//
//        ServletUtil.leggTilServlet(servletContext, new PopulerOppfolgingHistorikkServlet(oppfolgingsenhetHistorikkRepository, systemUserTokenProvider), "/internal/populer_enhet_historikk");
//        ServletUtil.leggTilServlet(servletContext, new PubliserHistorikkServlet(oppfolgingStatusKafkaProducer), "/internal/publiser_oppfolging_status_historikk");
//        ServletUtil.leggTilServlet(servletContext, new PubliserOppfolgingStatusServlet(oppfolgingStatusKafkaProducer), "/internal/publiser_oppfolging_status");

}
