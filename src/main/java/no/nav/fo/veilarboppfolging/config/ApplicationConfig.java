package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.migrateDatabase;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.resolveFromEnvironment;

@Configuration
@EnableScheduling
@ComponentScan(
        basePackages = "no.nav.fo.veilarboppfolging",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test")}
)
@Import(AktorConfig.class)
public class ApplicationConfig implements NaisApiApplication {

    public static final String APPLICATION_NAME = "veilarboppfolging";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY = "VEILARBAZUREADPROXY_DISCOVERY_URL";
    public static final String AAD_B2C_CLIENTID_USERNAME_PROPERTY = "AAD_B2C_CLIENTID_USERNAME";
    public static final String AAD_B2C_CLIENTID_PASSWORD_PROPERTY = "AAD_B2C_CLIENTID_PASSWORD";
    public static final String VEILARBAKTIVITETAPI_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String AKTIVITETSPLAN_URL_PROPERTY = "AKTIVITETSPLAN_URL";
    public static final String VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY = "VIRKSOMHET_YTELSESKONTRAKT_V3_ENDPOINTURL";
    public static final String VIRKSOMHET_OPPFOLGING_V1_PROPERTY = "VIRKSOMHET_OPPFOLGING_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_OPPFOELGINGSSTATUS_V2_PROPERTY = "VIRKSOMHET_OPPFOELGINGSSTATUS_V2_ENDPOINTURL";
    public static final String VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY = "VIRKSOMHET_ORGANISASJONENHET_V1_ENDPOINTURL";
    public static final String VARSELOPPGAVE_V1_PROPERTY = "VARSELOPPGAVE_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY = "VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ENDPOINTURL";
    public static final String KAFKA_BROKERS_PROPERTY = "KAFKA_BROKERS_URL";

    @Inject
    private DataSource dataSource;

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
        migrateDatabase(dataSource);
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
                .azureADB2CLogin()
                .issoLogin()
        ;
    }
}
