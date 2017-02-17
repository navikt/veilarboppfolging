package no.nav.fo.veilarbsituasjon.config;


import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon", excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MessageQueueConfig.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DatabaseConfig.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationConfig.class)})
public class ApplicationMockConfig {
}