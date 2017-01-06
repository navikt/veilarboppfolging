package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import org.springframework.context.annotation.Bean;

public class ServiceConfig {

    @Bean
    YtelseskontraktService ytelsesKontraktService() {
        return new YtelseskontraktService();
    }
}
