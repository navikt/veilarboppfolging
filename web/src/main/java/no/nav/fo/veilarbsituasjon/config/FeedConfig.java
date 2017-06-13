package no.nav.fo.veilarbsituasjon.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusFeedItem;
import no.nav.fo.veilarbsituasjon.services.OppfolgingStatusFeedProvider;
import no.nav.fo.veilarbsituasjon.services.TilordningFeedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;


@Configuration
public class FeedConfig {

    @Bean
    public FeedController feedController(
            FeedProducer<OppfolgingBruker> oppfolgingBrukerFeed,
            FeedProducer<OppfolgingStatusFeedItem> oppfolgingStatusFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed("tilordninger", oppfolgingBrukerFeed);
        feedServerController.addFeed("oppfolgingstatus", oppfolgingStatusFeed);

        return feedServerController;
    }

    @Bean
    public FeedProducer<OppfolgingBruker> oppfolgingBrukerFeed(BrukerRepository brukerRepository) {
        return FeedProducer.<OppfolgingBruker>builder()
                .provider(new TilordningFeedProvider(brukerRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .build();
    }

    @Bean
    public FeedProducer<OppfolgingStatusFeedItem> oppfolgingStatusFeed(SituasjonRepository situasjonRepository) {
        return FeedProducer.<OppfolgingStatusFeedItem>builder()
                .provider(new OppfolgingStatusFeedProvider(situasjonRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .build();
    }
}
