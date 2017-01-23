package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    private final YtelseskontraktV3 ytelseskontraktV3;

    public ServiceConfig(YtelseskontraktV3 ytelseskontraktV3) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
    }

    @Bean
    YtelseskontraktService ytelsesKontraktService() {
        return new YtelseskontraktService(ytelseskontraktV3);
    }
}
