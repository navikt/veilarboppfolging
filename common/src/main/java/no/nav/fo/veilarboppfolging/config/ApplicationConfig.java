package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarboppfolging.kafka.ConsumerConfig;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.fo.veilarboppfolging.kafka.Consumer.ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.*;

@Configuration
@EnableScheduling
@ComponentScan(
        basePackages = "no.nav.fo.veilarboppfolging",
        excludeFilters = { @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test")}
        )
@Import({AktorConfig.class,
        ConsumerConfig.class})
public class ApplicationConfig implements ApiApplication {

    public ApplicationConfig() {
        setProperty(ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME, "aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName(), PUBLIC);
    }

    private static final String STRING = "unleash.url";

    public static final String APPLICATION_NAME = "veilarboppfolging";

    @Bean
    public ConsumerConfig.SASL sasl(){
        return ConsumerConfig.SASL.ENABLED;
    }

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(STRING))
                .build());
    }

}