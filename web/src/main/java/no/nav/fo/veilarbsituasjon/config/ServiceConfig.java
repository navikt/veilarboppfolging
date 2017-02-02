package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.EndreVeilederService;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    private final YtelseskontraktV3 ytelseskontraktV3;
    private final OppfoelgingPortType oppfoelgingPortType;

    public ServiceConfig(YtelseskontraktV3 ytelseskontraktV3, OppfoelgingPortType oppfoelgingPortType) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    @Bean
    YtelseskontraktService ytelsesKontraktService() {
        return new YtelseskontraktService(ytelseskontraktV3);
    }

    @Bean
    OppfoelgingService oppfoelgingService() {
        return new OppfoelgingService(oppfoelgingPortType);
    }

    @Bean
    AktoerIdService aktoerIdService() { return new AktoerIdService(); }

    @Bean
    EndreVeilederService endreVeilederService() { return new EndreVeilederService(); }
}
