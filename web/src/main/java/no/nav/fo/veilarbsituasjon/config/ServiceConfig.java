package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.repository.AktoerIdToVeilederDAO;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    private final YtelseskontraktV3 ytelseskontraktV3;
    private final OppfoelgingPortType oppfoelgingPortType;
    private AktoerV2 aktoerV2;
    private SessionFactory sessionFactory;

    public ServiceConfig(YtelseskontraktV3 ytelseskontraktV3, OppfoelgingPortType oppfoelgingPortType, AktoerV2 aktoerV2, SessionFactory sessionFactory) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.aktoerV2 = aktoerV2;
        this.sessionFactory = sessionFactory;
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
    AktoerIdService aktoerIdService() { return new AktoerIdService(aktoerV2); }

    @Bean
    AktoerIdToVeilederDAO aktoerIdToVeilederDAO() { return new AktoerIdToVeilederDAO(sessionFactory); }
}
