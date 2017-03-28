package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.services.*;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ServiceConfig {

    private final YtelseskontraktV3 ytelseskontraktV3;
    private final OppfoelgingPortType oppfoelgingPortType;
    private final OrganisasjonEnhetV1 organisasjonEnhetV1;
    private AktoerV2 aktoerV2;
    private JdbcTemplate db;

    public ServiceConfig(YtelseskontraktV3 ytelseskontraktV3, OppfoelgingPortType oppfoelgingPortType, AktoerV2 aktoerV2, JdbcTemplate db, OrganisasjonEnhetV1 organisasjonEnhetV1) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.aktoerV2 = aktoerV2;
        this.db = db;
        this.organisasjonEnhetV1 = organisasjonEnhetV1;
    }

    @Bean
    YtelseskontraktService ytelsesKontraktService() {
        return new YtelseskontraktService(ytelseskontraktV3);
    }

    @Bean
    OrganisasjonsenhetService organisasjonsenhetService() {
        return new OrganisasjonsenhetService(organisasjonEnhetV1);
    }

    @Bean
    OppfolgingService oppfolgingService() {
        return new OppfolgingService(oppfoelgingPortType, organisasjonsenhetService());
    }

    @Bean
    AktoerIdService aktoerIdService() {
        return new AktoerIdService(aktoerV2);
    }

    @Bean
    BrukerRepository brukerRepository() {
        return new BrukerRepository(db);
    }

    @Bean
    SituasjonRepository situasjonRepository() {
        return new SituasjonRepository(db);
    }

}