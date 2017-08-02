package no.nav.fo.veilarbsituasjon.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbsituasjon.services.AvsluttetOppfolgingFeedProvider;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.fo.veilarbsituasjon.services.SituasjonFeedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;


@Configuration
public class FeedConfig {

    @Bean
    public FeedController feedController(
            FeedProducer<OppfolgingBruker> oppfolgingBrukerFeed,
            FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed(OppfolgingBruker.FEED_NAME, oppfolgingBrukerFeed);
        feedServerController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeed);

        return feedServerController;
    }

    @Bean
    public FeedProducer<OppfolgingBruker> oppfolgingBrukerFeed(BrukerRepository brukerRepository) {
        return FeedProducer.<OppfolgingBruker>builder()
                .provider(new SituasjonFeedProvider(brukerRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .build();
    }

    @Bean
    public FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttOppfolgingFeed(SituasjonOversiktService situasjonOversiktService) {
        return FeedProducer.<AvsluttetOppfolgingFeedDTO>builder()
                .provider(new AvsluttetOppfolgingFeedProvider(situasjonOversiktService))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .build();
    }
}
