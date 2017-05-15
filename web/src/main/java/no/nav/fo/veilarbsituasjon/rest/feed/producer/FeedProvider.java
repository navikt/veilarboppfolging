package no.nav.fo.veilarbsituasjon.rest.feed.producer;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedProvider<T> {
    List<FeedElement<T>> hentData(LocalDateTime sinceId, int pageSize);
}
