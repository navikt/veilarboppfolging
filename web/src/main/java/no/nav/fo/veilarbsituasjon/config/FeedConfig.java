package no.nav.fo.veilarbsituasjon.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.AvsluttOppfolgingFeedItem;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.services.AvsluttOppfolgingFeedProvider;
import no.nav.fo.veilarbsituasjon.services.TilordningFeedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;


@Configuration
public class FeedConfig {

    @Bean
    public FeedController feedController(
            FeedProducer<OppfolgingBruker> oppfolgingBrukerFeed,
            FeedProducer<AvsluttOppfolgingFeedItem> oppfolgingStatusFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed("tilordninger", oppfolgingBrukerFeed);
        feedServerController.addFeed("avsluttoppfolging", oppfolgingStatusFeed);

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
    public FeedProducer<AvsluttOppfolgingFeedItem> avsluttOppfolgingFeed(SituasjonRepository situasjonRepository) {
        return FeedProducer.<AvsluttOppfolgingFeedItem>builder()
                .provider(new AvsluttOppfolgingFeedProvider(situasjonRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .build();
    }
}
