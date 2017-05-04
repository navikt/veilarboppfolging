package no.nav.fo.veilarbsituasjon.config;


import no.nav.fo.veilarbsituasjon.mock.TilordningServiceMock;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackages = "no.nav.fo.veilarbsituasjon",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DatabaseConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationConfig.class)
        }
)
public class ApplicationMockConfig {
        @Bean
        TilordningService tilordningService() {
                return new TilordningServiceMock();
        }
}