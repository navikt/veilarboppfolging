package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(
        basePackages = "no.nav.fo.veilarboppfolging",
        excludeFilters = { @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test")}
        )
@Import(AktorConfig.class)
public class ApplicationConfig implements ApiApplication {

    private static final String STRING = "unleash.url";

    public static final String APPLICATION_NAME = "veilarboppfolging";

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(STRING))
                .build());
    }

}