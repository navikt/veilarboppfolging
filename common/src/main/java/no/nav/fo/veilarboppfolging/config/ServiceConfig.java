package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OrganisasjonEnhetService;
import no.nav.fo.veilarboppfolging.services.YtelseskontraktService;
import no.nav.sbl.jdbc.Database;
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
    OrganisasjonEnhetService organisasjonsenhetService(OrganisasjonEnhetV1 organisasjonEnhetV1) {
        return new OrganisasjonEnhetService(organisasjonEnhetV1);
    }

    @Bean
    ArenaOppfolgingService arenaOppfolgingService(OppfoelgingPortType oppfoelgingPortType,
                                                  OrganisasjonEnhetService organisasjonEnhetService) {
        return new ArenaOppfolgingService(oppfoelgingPortType, organisasjonEnhetService);
    }

    @Bean
    OppfolgingFeedRepository oppfolgingFeedRepository(JdbcTemplate db) {
        return new OppfolgingFeedRepository(db);
    }

    @Bean
    VeilederTilordningerRepository veilederTilordningerRepository(JdbcTemplate db,
                                                                  OppfolgingRepository oppfolgingRepository) {
        return new VeilederTilordningerRepository(db, oppfolgingRepository);
    }

    @Bean
    OppfolgingRepository oppfolgingRepository(Database db) {
        return new OppfolgingRepository(db);
    }

    @Bean
    KvpRepository kvpRepository(Database db) {
        return new KvpRepository(db);
    }

}