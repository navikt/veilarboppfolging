package no.nav.veilarboppfolging.feed;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.cjm.common.Authorization;
import no.nav.veilarboppfolging.feed.cjm.common.FeedRequest;
import no.nav.veilarboppfolging.feed.cjm.common.FeedResponse;
import no.nav.veilarboppfolging.feed.cjm.common.FeedWebhookRequest;
import no.nav.veilarboppfolging.feed.cjm.exception.MissingIdException;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.feed.FeedConfig.*;
import static no.nav.veilarboppfolging.feed.cjm.util.MetricsUtils.timed;
import static no.nav.veilarboppfolging.feed.cjm.util.UrlUtils.QUERY_PARAM_ID;
import static no.nav.veilarboppfolging.feed.cjm.util.UrlUtils.QUERY_PARAM_PAGE_SIZE;

@Slf4j
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private Map<String, FeedProducer> producers = new HashMap<>();

    public FeedController(
            FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
            FeedProducer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeed,
            FeedProducer<KvpDTO> kvpFeed,
            FeedProducer<NyeBrukereFeedDTO> nyeBrukereFeed
    ) {
        log.info("starter");
        addFeed(OPPFOLGING_FEED_NAME, oppfolgingFeed);
        addFeed(AVSLUTTET_OPPFOLGING_FEED_NAME, avsluttetOppfolgingFeed);
        addFeed(KVP_FEED_NAME, kvpFeed);
        addFeed(NYE_BRUKERE_FEED_NAME, nyeBrukereFeed);
    }

    <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> FeedController addFeed(String serverFeedname, FeedProducer<DOMAINOBJECT> producer) {
        log.info("ny feed. navn={}", serverFeedname);
        producers.put(serverFeedname, producer);
        return this;
    }

    @GetMapping
    public List<String> getFeeds() {
        return new ArrayList<>(producers.keySet());
    }

    @PutMapping("/{name}/webhook")
    public Response registerWebhook(FeedWebhookRequest request, @PathVariable("name") String name) {
        return timed(String.format("feed.%s.createwebhook", name), () -> ofNullable(producers.get(name))
                .map((producer) -> authorizeRequest(producer, name))
                .map((feed) -> feed.createWebhook(request))
                .map((created) -> Response.status(created ? 201 : 200))
                .orElse(Response.status(Response.Status.BAD_REQUEST)).build());
    }

    @GetMapping("/{name}")
    public FeedResponse<?> getFeeddata(
            @PathVariable("name") String name,
            @RequestParam(QUERY_PARAM_ID) String id,
            @RequestParam(QUERY_PARAM_PAGE_SIZE) Integer pageSize
    ) {
        return timed(String.format("feed.%s.poll", name), () -> {
            FeedProducer feedProducer = ofNullable(producers.get(name)).orElseThrow(NotFoundException::new);
            authorizeRequest(feedProducer, name);
            FeedRequest request = new FeedRequest()
                    .setSinceId(ofNullable(id).orElseThrow(MissingIdException::new))
                    .setPageSize(ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE));
            return feedProducer.getFeedPage(name, request);
        });
    }

    @GetMapping("/feedname")
    public Set<String> getFeedNames() {
        return producers.keySet();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return feed;
    }

}
