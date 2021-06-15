package no.nav.veilarboppfolging.feed;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.feed.cjm.common.Authorization;
import no.nav.veilarboppfolging.feed.cjm.common.FeedRequest;
import no.nav.veilarboppfolging.feed.cjm.common.FeedResponse;
import no.nav.veilarboppfolging.feed.cjm.common.FeedWebhookRequest;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.feed.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.domain.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.feed.domain.OppfolgingFeedDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.feed.FeedConfig.*;
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
    public ResponseEntity registerWebhook(@RequestBody FeedWebhookRequest request, @PathVariable("name") String name) {
        var maybeIsCreated = ofNullable(producers.get(name))
                .map((producer) -> authorizeRequest(producer, name))
                .map((feed) -> feed.createWebhook(request));

        if (maybeIsCreated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(maybeIsCreated.get() ? 201 : 200).build();
    }

    @GetMapping("/{name}")
    public FeedResponse<?> getFeeddata(
            @PathVariable("name") String name,
            @RequestParam(QUERY_PARAM_ID) String id,
            @RequestParam(QUERY_PARAM_PAGE_SIZE) Integer pageSize
    ) {
        FeedProducer feedProducer = ofNullable(producers.get(name)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        authorizeRequest(feedProducer, name);
        FeedRequest request = new FeedRequest()
                .setSinceId(ofNullable(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request må inneholde id")))
                .setPageSize(ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE));
        return feedProducer.getFeedPage(name, request);
    }

    @GetMapping("/feedname")
    public Set<String> getFeedNames() {
        return producers.keySet();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return feed;
    }

}
