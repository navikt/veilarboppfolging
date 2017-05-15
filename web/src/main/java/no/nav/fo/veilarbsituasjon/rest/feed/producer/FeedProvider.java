package no.nav.fo.veilarbsituasjon.rest.feed.producer;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedProvider<E, T> {
    List<FeedElement<E, T>> hentData(LocalDateTime sinceId, int pageSize);
}
