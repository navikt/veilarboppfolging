package no.nav.fo.veilarbsituasjon.config;

import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
@Import({ServiceConfig.class,
        ArenaServiceConfig.class})
public class ApplicationConfig {

}