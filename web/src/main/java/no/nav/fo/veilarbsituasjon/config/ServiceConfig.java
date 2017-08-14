package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.services.*;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class ServiceConfig {

    private final YtelseskontraktV3 ytelseskontraktV3;
    private final OppfoelgingPortType oppfoelgingPortType;
    private final OrganisasjonEnhetV1 organisasjonEnhetV1;
    private final JmsTemplate endreVeilederQueue;
    private final BrukerRepository brukerRepository;
    private AktoerV2 aktoerV2;
    private JdbcTemplate db;
    private final Pep pep;

    public ServiceConfig(YtelseskontraktV3 ytelseskontraktV3, OppfoelgingPortType oppfoelgingPortType, AktoerV2 aktoerV2, JdbcTemplate db, OrganisasjonEnhetV1 organisasjonEnhetV1, Pep pep,
                         JmsTemplate endreVeilederQueue, BrukerRepository brukerRepository) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.aktoerV2 = aktoerV2;
        this.db = db;
        this.pep = pep;
        this.organisasjonEnhetV1 = organisasjonEnhetV1;
        this.endreVeilederQueue = endreVeilederQueue;
        this.brukerRepository = brukerRepository;
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

    @Bean
    PepClient pepClient() {
        return new PepClient(pep);
    }

    @Bean
    TilordningService tilordningService() {
        return new TilordningService(endreVeilederQueue, brukerRepository);
    }
}