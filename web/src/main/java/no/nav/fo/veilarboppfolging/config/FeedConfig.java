package no.nav.fo.veilarboppfolging.config;

import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.services.AvsluttetOppfolgingFeedProvider;
import no.nav.fo.veilarboppfolging.services.KvpFeedProvider;
import no.nav.fo.veilarboppfolging.services.OppfolgingFeedProvider;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;


@Configuration
public class FeedConfig {
    public static final String OPPFOLGING_FEED_NAME = "oppfolging";
    public static final String AVSLUTTET_OPPFOLGING_FEED_NAME = "avsluttetoppfolging";
    public static final String KVP_FEED_NAME = "kvp";

    @Bean
    public FeedController feedController(
            FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
            FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeed,
            FeedProducer<KvpDTO> kvpFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed(OPPFOLGING_FEED_NAME, oppfolgingFeed);
        feedServerController.addFeed(AVSLUTTET_OPPFOLGING_FEED_NAME, avsluttetOppfolgingFeed);
        feedServerController.addFeed(KVP_FEED_NAME, kvpFeed);

        return feedServerController;
    }

    @Bean
    public FeedProducer<OppfolgingFeedDTO> oppfolgingFeed(OppfolgingFeedRepository oppfolgingFeedRepository) {
        return FeedProducer.<OppfolgingFeedDTO>builder()
                .provider(new OppfolgingFeedProvider(oppfolgingFeedRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }

    @Bean
    public FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttOppfolgingFeed(OppfolgingService oppfolgingService) {
        return FeedProducer.<AvsluttetOppfolgingFeedDTO>builder()
                .provider(new AvsluttetOppfolgingFeedProvider(oppfolgingService))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }

    @Bean
    public FeedProducer<KvpDTO> kvpFeed(KvpRepository repo) {
        return FeedProducer.<KvpDTO>builder()
                .provider(new KvpFeedProvider(repo))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()) )
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }
}
