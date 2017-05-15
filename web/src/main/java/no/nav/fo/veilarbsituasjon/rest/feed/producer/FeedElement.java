package no.nav.fo.veilarbsituasjon.rest.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedElement<T> {
    protected String id;
    protected T data;
}
