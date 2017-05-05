package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.Value;

import java.util.List;

@Value
class FeedResponse {
    List<FeedElement> elements;
}
