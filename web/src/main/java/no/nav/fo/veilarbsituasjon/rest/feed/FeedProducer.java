package no.nav.fo.veilarbsituasjon.rest.feed;

import javaslang.control.Try;
import lombok.Builder;
import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import no.nav.fo.veilarbsituasjon.exception.HttpNotSupportedException;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.LinkedList;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;
import static no.nav.fo.veilarbsituasjon.rest.feed.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer {

    private static final Logger LOG = getLogger(FeedProducer.class);

    private int maxPageSize;
    private String webhookUrl;
    private String callbackUrl;

    public Response createFeedResponse(FeedRequest request, TilordningService service) {
        LinkedList<Tilordning> feedElements = service.hentTilordninger(LocalDateTime.now());
        return Response.ok().entity(feedElements).build();
    }

    public void activateWebhook() {

        OkHttpClient okHttpClient = new OkHttpClient();

        Try.of(() -> {
            Request request = new Request.Builder().url(webhookUrl).build();
            okhttp3.Response response = okHttpClient.newCall(request).execute();
            return response.body().string();
        }).onFailure(e -> LOG.warn("Det skjedde en feil ved aktivering av webhook", e.getMessage()));
    }

    public Response getWebhook() {
        return Response.ok().entity(webhookUrl).build();
    }

    public Response createWebhook() {
        if (webhookUrl.equals(callbackUrl)) {
            return Response.ok().build();
        }

        Try.of(() -> {
            validateUrl(callbackUrl);
            webhookUrl = callbackUrl;
            URI uri = new URI("tilordninger/webhook");
            return Response.created(uri).build();

        }).recover(e -> Match(e).of(
                Case(instanceOf(URISyntaxException.class),
                        Response.serverError().entity("Det skjedde en feil web opprettelsen av webhook").build()),
                Case(instanceOf(MalformedURLException.class),
                        Response.status(400).entity("Feil format på callback-url").build()),
                Case(instanceOf(HttpNotSupportedException.class),
                        Response.status(400).entity("Angitt url for webhook må være HTTPS").build())
        ));

        return Response.status(500).entity("Det skjedde en feil ved opprettelse av webhook. Prøv igjen senere").build();
    }
}
