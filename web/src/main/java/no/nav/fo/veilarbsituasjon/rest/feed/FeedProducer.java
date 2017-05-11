package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.Builder;
import lombok.SneakyThrows;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.feed.exception.NoCallbackUrlException;
import no.nav.fo.veilarbsituasjon.rest.feed.exception.NoWebhookUrlException;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.veilarbsituasjon.rest.feed.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer {

    private static final Logger LOG = getLogger(FeedProducer.class);

    private int maxPageSize;
    private Optional<String> webhookUrl;
    private Optional<String> callbackUrl;

    public Response createFeedResponse(FeedRequest request, TilordningService service) {
        int pageSize = setPageSize(request.pageSize, maxPageSize);
        LocalDateTime sinceId = LocalDateTime.parse(request.sinceId, DateTimeFormatter.ISO_DATE_TIME);
        List<OppfolgingBruker> feedElements = service.hentTilordninger(sinceId, pageSize);
        return Response.ok().entity(feedElements).build();
    }

    private static int setPageSize(int pageSize, int maxPageSize) {
        return pageSize > maxPageSize ? maxPageSize : pageSize;
    }

    public void activateWebhook() {
        webhookUrl.ifPresent(
                url -> {
                    Client client = ClientBuilder.newBuilder().build();
                    client.target(url).request().build(HEAD).invoke();
                }
        );
    }

    public Response getWebhook() {
        String url = webhookUrl.orElseThrow(NoWebhookUrlException::new);
        return Response.ok().entity(new FeedWebhookResponse().setWebhookUrl(url)).build();
    }

    public Response createWebhook(Optional<String> callbackUrl) {
        if (callbackUrl.equals(webhookUrl)) {
            return Response.ok().build();
        }
        String url = callbackUrl.orElseThrow(NoCallbackUrlException::new);
        validateUrl(url);

        webhookUrl = callbackUrl;
        return Response.created(getUri()).build();
    }

    @SneakyThrows
    private URI getUri() {
        return new URI("tilordninger/webhook");
    }
}
