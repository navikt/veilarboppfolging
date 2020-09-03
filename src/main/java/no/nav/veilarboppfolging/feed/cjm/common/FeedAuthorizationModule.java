package no.nav.veilarboppfolging.feed.cjm.common;

@FunctionalInterface
public interface FeedAuthorizationModule {
    boolean isRequestAuthorized(String feedname);
}
