package no.nav.fo.veilarbsituasjon.rest.feed.producer;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
class FeedResponse {
    ZonedDateTime nextId;
    List<FeedElement> elements;
}
