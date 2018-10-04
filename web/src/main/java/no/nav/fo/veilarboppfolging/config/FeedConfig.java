package no.nav.fo.veilarboppfolging.config;

import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.NyeBrukereFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.services.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;


@Configuration
public class FeedConfig {
    public static final String OPPFOLGING_FEED_NAME = "oppfolging";
    public static final String OPPFOLGING_MED_LOPENUMMER_FEED_NAME = "oppfolgingmedlopenummer";
    public static final String AVSLUTTET_OPPFOLGING_FEED_NAME = "avsluttetoppfolging";
    public static final String NYE_BRUKERE_FEED_NAME = "nyebrukere";

    @Bean
    public FeedController feedController(
            @Qualifier("oppfolgingFeed") FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
            @Qualifier("oppfolgingMedLopenummerFeed") FeedProducer<OppfolgingFeedDTO> oppfolgingMedLopenummerFeed,
            FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeed,
            FeedProducer<KvpDTO> kvpFeed,
            FeedProducer<NyeBrukereFeedDTO> nyeBrukereFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed(OPPFOLGING_FEED_NAME, oppfolgingFeed);
        feedServerController.addFeed(OPPFOLGING_MED_LOPENUMMER_FEED_NAME, oppfolgingMedLopenummerFeed);
        feedServerController.addFeed(AVSLUTTET_OPPFOLGING_FEED_NAME, avsluttetOppfolgingFeed);
        feedServerController.addFeed(KvpDTO.FEED_NAME, kvpFeed);
        feedServerController.addFeed(NYE_BRUKERE_FEED_NAME, nyeBrukereFeed);

        return feedServerController;
    }

    @Bean(name="oppfolgingFeed")
    public FeedProducer<OppfolgingFeedDTO> oppfolgingFeed(OppfolgingFeedRepository oppfolgingFeedRepository) {
        return FeedProducer.<OppfolgingFeedDTO>builder()
                .provider(new OppfolgingFeedProvider(oppfolgingFeedRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }

    @Bean("oppfolgingMedLopenummerFeed")
    public FeedProducer<OppfolgingFeedDTO> oppfolgingMedLopenummerFeed(OppfolgingFeedRepository oppfolgingFeedRepository) {
        return FeedProducer.<OppfolgingFeedDTO>builder()
                .provider(new OppfolgingFeedMedLopenummerProvider(oppfolgingFeedRepository))
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

    @Bean
    public FeedProducer<NyeBrukereFeedDTO> nyeBrukereFeed(NyeBrukereFeedRepository repo) {
        return FeedProducer.<NyeBrukereFeedDTO>builder()
                .provider((id, pageSize) -> repo.hentElementerStorreEnnId(id, pageSize).stream().map(NyeBrukereFeedDTO::toFeedElement))
                .maxPageSize(1000)
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }
}
