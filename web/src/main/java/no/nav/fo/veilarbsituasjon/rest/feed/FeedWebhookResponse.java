package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedWebhookResponse {
    private String melding;
    public String webhookUrl;
}
