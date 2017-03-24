package no.nav.fo.veilarbsituasjon.config;

import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
@Import({ServiceConfig.class, ArenaServiceConfig.class, AbacContext.class})
public class ApplicationConfig {

}