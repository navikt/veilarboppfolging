package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.db.*;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OrganisasjonEnhetService;
import no.nav.fo.veilarboppfolging.services.YtelseskontraktService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.jdbc.Database;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
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
    OrganisasjonEnhetService organisasjonsenhetService(OrganisasjonEnhetV2 organisasjonEnhetV2) {
        return new OrganisasjonEnhetService(organisasjonEnhetV2);
    }

    @Bean
    ArenaOppfolgingService arenaOppfolgingService(OppfoelgingsstatusV1 oppfoelgingsstatusV1,
                                                  OppfoelgingsstatusV2 oppfoelgingsstatusV2,
                                                  OppfoelgingPortType oppfoelgingPortType,
                                                  UnleashService unleashService) {

        return new ArenaOppfolgingService(oppfoelgingsstatusV1, oppfoelgingsstatusV2, oppfoelgingPortType, unleashService);
    }

    @Bean
    OppfolgingFeedRepository oppfolgingFeedRepository(JdbcTemplate db) {
        return new OppfolgingFeedRepository(db);
    }

    @Bean
    VeilederTilordningerRepository veilederTilordningerRepository(Database db,
                                                                  OppfolgingRepository oppfolgingRepository) {
        return new VeilederTilordningerRepository(db, oppfolgingRepository);
    }

    @Bean
    OppfolgingRepository oppfolgingRepository(Database db) {
        return new OppfolgingRepository(db);
    }

    @Bean
    OppfolgingsStatusRepository oppfolgingsStatusRepository(Database db) {
        return new OppfolgingsStatusRepository(db);
    }

    @Bean
    KvpRepository kvpRepository(Database db) {
        return new KvpRepository(db);
    }

    @Bean
    NyeBrukereFeedRepository nyeBrukereFeedRepository(Database db) {
        return new NyeBrukereFeedRepository(db);
    }
}