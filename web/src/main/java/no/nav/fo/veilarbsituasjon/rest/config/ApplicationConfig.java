package no.nav.fo.veilarbsituasjon.rest.config;

import no.nav.fo.veilarbsituasjon.config.ArenaServiceConfig;
import no.nav.fo.veilarbsituasjon.config.ServiceConfig;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
@Import({ServiceConfig.class,
        ArenaServiceConfig.class})
public class ApplicationConfig {

}