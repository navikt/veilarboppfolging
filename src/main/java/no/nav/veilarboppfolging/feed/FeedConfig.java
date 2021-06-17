package no.nav.veilarboppfolging.feed;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.sts.OpenAmSystemUserTokenProvider;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.feed.cjm.common.OidcFeedAuthorizationModule;
import no.nav.veilarboppfolging.feed.cjm.common.OidcFeedOutInterceptor;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.feed.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.domain.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.feed.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.provider.AvsluttetOppfolgingFeedProvider;
import no.nav.veilarboppfolging.feed.provider.KvpFeedProvider;
import no.nav.veilarboppfolging.feed.provider.OppfolgingFeedProvider;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.NyeBrukereFeedRepository;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static java.util.Collections.singletonList;

@Configuration
public class FeedConfig {

    static final String OPPFOLGING_FEED_NAME = "oppfolging";
    static final String AVSLUTTET_OPPFOLGING_FEED_NAME = "avsluttetoppfolging";
    static final String NYE_BRUKERE_FEED_NAME = "nyebrukere";
    static final String KVP_FEED_NAME = "kvp";

    private final AuthContextHolder authContextHolder;

    private final OpenAmSystemUserTokenProvider openAmSystemUserTokenProvider;

    @Autowired
    public FeedConfig(AuthContextHolder authContextHolder, OpenAmSystemUserTokenProvider openAmSystemUserTokenProvider) {
        this.authContextHolder = authContextHolder;
        this.openAmSystemUserTokenProvider = openAmSystemUserTokenProvider;
    }

    @Bean
    public FeedProducer<OppfolgingFeedDTO> oppfolgingFeed(OppfolgingFeedRepository oppfolgingFeedRepository) {
        List<String> oppfolgingFeedAllowedUsers = List.of("srvpam-cv-api");
        return FeedProducer.<OppfolgingFeedDTO>builder()
                .provider(new OppfolgingFeedProvider(oppfolgingFeedRepository))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor(openAmSystemUserTokenProvider)))
                .authorizationModule(new OidcFeedAuthorizationModule(authContextHolder, oppfolgingFeedAllowedUsers))
                .build();
    }

    @Bean
    public FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttOppfolgingFeed(OppfolgingService oppfolgingService) {
        List<String> avsluttOppfolgingFeedAllowedUsers = List.of("srvveilarbdialog", "srvveilarbaktivitet", "srvveilarbjobbsoke");
        return FeedProducer.<AvsluttetOppfolgingFeedDTO>builder()
                .provider(new AvsluttetOppfolgingFeedProvider(oppfolgingService))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor(openAmSystemUserTokenProvider)) )
                .authorizationModule(new OidcFeedAuthorizationModule(authContextHolder, avsluttOppfolgingFeedAllowedUsers))
                .build();
    }

    @Bean
    public FeedProducer<KvpDTO> kvpFeed(KvpRepository repo) {
        List<String> kvpFeedAllowedUsers = List.of("srvveilarbdialog", "srvveilarbaktivitet");
        return FeedProducer.<KvpDTO>builder()
                .provider(new KvpFeedProvider(repo))
                .maxPageSize(1000)
                .interceptors(singletonList(new OidcFeedOutInterceptor(openAmSystemUserTokenProvider)) )
                .authorizationModule(new OidcFeedAuthorizationModule(authContextHolder, kvpFeedAllowedUsers))
                .build();
    }

    @Bean
    public FeedProducer<NyeBrukereFeedDTO> nyeBrukereFeed(NyeBrukereFeedRepository repo) {
        List<String> nyeBrukereFeedAllowedUsers = List.of("srvveilarbdirigent");
        return FeedProducer.<NyeBrukereFeedDTO>builder()
                .provider((id, pageSize) -> repo.hentElementerStorreEnnId(id, pageSize).stream().map(NyeBrukereFeedDTO::toFeedElement))
                .maxPageSize(1000)
                .authorizationModule(new OidcFeedAuthorizationModule(authContextHolder, nyeBrukereFeedAllowedUsers))
                .build();
    }
}
