package no.nav.veilarboppfolging.feed;

import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.veilarboppfolging.controller.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.services.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;

@Configuration
public class FeedConfig {

    public static final String OPPFOLGING_FEED_NAME = "oppfolging";
    public static final String AVSLUTTET_OPPFOLGING_FEED_NAME = "avsluttetoppfolging";
    public static final String NYE_BRUKERE_FEED_NAME = "nyebrukere";
    public static final String KVP_FEED_NAME = "kvp";

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

    @Bean
    public FeedProducer<NyeBrukereFeedDTO> nyeBrukereFeed(NyeBrukereFeedRepository repo) {
        return FeedProducer.<NyeBrukereFeedDTO>builder()
                .provider((id, pageSize) -> repo.hentElementerStorreEnnId(id, pageSize).stream().map(NyeBrukereFeedDTO::toFeedElement))
                .maxPageSize(1000)
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }
}
