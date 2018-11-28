package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.db.*;

import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OrganisasjonEnhetService;
import no.nav.fo.veilarboppfolging.services.YtelseskontraktService;
import no.nav.sbl.jdbc.Database;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;

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
    ArenaOppfolgingService arenaOppfolgingService(OppfoelgingsstatusV2 oppfoelgingsstatusV2,
                                                  OppfoelgingPortType oppfoelgingPortType) {

        return new ArenaOppfolgingService(oppfoelgingsstatusV2, oppfoelgingPortType);
    }

    @Bean
    OppfolgingFeedRepository oppfolgingFeedRepository(JdbcTemplate db, LockingTaskExecutor taskExecutor) {
        return new OppfolgingFeedRepository(db, taskExecutor);
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
    
    @Bean
    public LockingTaskExecutor taskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }

}