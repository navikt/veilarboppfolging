package no.nav.veilarboppfolging.feed.cjm.producer;

import lombok.Builder;
import no.nav.common.rest.client.RestClient;
import no.nav.veilarboppfolging.feed.cjm.common.*;
import no.nav.veilarboppfolging.feed.cjm.util.MetricsUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static no.nav.veilarboppfolging.feed.cjm.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> implements Authorization {

    private static final Logger LOG = getLogger(FeedProducer.class);
    private static final OkHttpClient client = RestClient.baseClient();

    @Builder.Default
    private int maxPageSize = 10000;

    // Trådsikkert Set. Er final slik at ikke brukere av FeedProducer kan velge å sette inn et annet Set som 
    // ikke er trådsikkert. Det bør ikke være noen grunn til at de som bruker FeedProducer skal behøve å forholde 
    // seg direkte til dette Set-et.
    private final Set<String> callbackUrls = ConcurrentHashMap.newKeySet();

    @Builder.Default
    private List<OutInterceptor> interceptors = new ArrayList<>();

    @Builder.Default
    private FeedAuthorizationModule authorizationModule = (feedname) -> true;

    private FeedProvider<DOMAINOBJECT> provider;

    public FeedResponse<DOMAINOBJECT> getFeedPage(String feedname, FeedRequest request) {
        int pageSize = getPageSize(request.getPageSize(), maxPageSize);
        String id = request.getSinceId();

        List<FeedElement<DOMAINOBJECT>> pageElements = provider
                .fetchData(id, pageSize)
                .sorted()
                .limit(pageSize + 1) // dette er med god grunn: se fetchnotlimited og FeedControllerTester
                .collect(Collectors.toList());

        if (pageElements.size() > pageSize) {
            MetricsUtils.metricEvent("fetchnotlimited", feedname);
            LOG.warn("Provider retrieved more than <pageSize> elements in response to {} for feed {}", request, feedname);
            LOG.info("This can lead to excessive resource consumption by the producer...");
        }

        long antallUnikeIder = pageElements
                .stream()
                .map(FeedElement::getId)
                .distinct()
                .count();

        if (pageElements.size() != antallUnikeIder) {
            MetricsUtils.metricEvent("duplicateid", feedname);
            LOG.warn("Found duplicate IDs in response to {} for feed {}", request, feedname);
            LOG.info("This can lead to excessive network usage between the producer and its consumers...");
        }

        if (pageElements.isEmpty()) {
            return new FeedResponse<DOMAINOBJECT>().setNextPageId(id);
        }

        String nextPageId = Optional.ofNullable(pageElements.get(pageElements.size() - 1))
                .map(FeedElement::getId)
                .orElse(null);

        return new FeedResponse<DOMAINOBJECT>().setNextPageId(nextPageId).setElements(pageElements);
    }
    
    public Map<String, Future<Integer>> activateWebhook() {
        return callbackUrls
                .stream()
                .collect(toMap(Function.identity(), str -> CompletableFuture.supplyAsync(() -> tryActivateWebHook(str))));
    }

    private int tryActivateWebHook(String url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .head();

        this.interceptors.forEach(interceptor -> interceptor.apply(requestBuilder));

        try (Response response = client.newCall(requestBuilder.build()).execute()) {

            if (response.code() != 200) {
                LOG.warn("Fikk ikke forventet status fra kall til webhook. Url {}, returnert status {}", url, response.code());
            }

            return response.code();
        } catch (Exception e) {
            LOG.warn("Feil ved activate webhook til url {}, {}", url, e.getMessage(), e);
            return 500;
        }
    }

    public boolean createWebhook(FeedWebhookRequest request) {
        return Optional.ofNullable(request.callbackUrl)
                .map(this::createWebhook)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid url"));
    }

    @Override
    public FeedAuthorizationModule getAuthorizationModule() {
        return authorizationModule;
    }

    private boolean createWebhook(String callbackUrl) {
        validateUrl(callbackUrl);
        return callbackUrls.add(callbackUrl);
    }

    private static int getPageSize(int pageSize, int maxPageSize) {
        return Math.min(pageSize, maxPageSize);
    }
}
