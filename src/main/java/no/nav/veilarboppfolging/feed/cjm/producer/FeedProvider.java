package no.nav.veilarboppfolging.feed.cjm.producer;

import no.nav.veilarboppfolging.feed.cjm.common.FeedElement;

import java.util.stream.Stream;

@FunctionalInterface
public interface FeedProvider<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {
    Stream<FeedElement<DOMAINOBJECT>> fetchData(String id, int pageSize);
}
