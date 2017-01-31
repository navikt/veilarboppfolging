package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.config.ArenaServiceConfig;
import no.nav.fo.veilarbsituasjon.config.MessageQueueConfig;
import no.nav.fo.veilarbsituasjon.config.ServiceConfig;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon", excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MessageQueueConfig.class)})
@Import({ServiceConfig.class,
        ArenaServiceConfig.class})
public class ApplicationMockConfig {

}