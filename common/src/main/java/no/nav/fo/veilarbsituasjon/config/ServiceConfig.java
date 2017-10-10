package no.nav.fo.veilarbsituasjon.config;

import no.nav.sbl.jdbc.Database;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.services.OppfolgingService;
import no.nav.fo.veilarbsituasjon.services.OrganisasjonsenhetService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ServiceConfig {

    @Bean
    YtelseskontraktService ytelsesKontraktService(YtelseskontraktV3 ytelseskontraktV3) {
        return new YtelseskontraktService(ytelseskontraktV3);
    }

    @Bean
    OrganisasjonsenhetService organisasjonsenhetService(OrganisasjonEnhetV1 organisasjonEnhetV1) {
        return new OrganisasjonsenhetService(organisasjonEnhetV1);
    }

    @Bean
    OppfolgingService oppfolgingService(OppfoelgingPortType oppfoelgingPortType, OrganisasjonsenhetService organisasjonsenhetService) {
        return new OppfolgingService(oppfoelgingPortType, organisasjonsenhetService);
    }

    @Bean
    BrukerRepository brukerRepository(JdbcTemplate db, SituasjonRepository situasjonRepository) {
        return new BrukerRepository(db, situasjonRepository);
    }

    @Bean
    SituasjonRepository situasjonRepository(Database db) {
        return new SituasjonRepository(db);
    }

}