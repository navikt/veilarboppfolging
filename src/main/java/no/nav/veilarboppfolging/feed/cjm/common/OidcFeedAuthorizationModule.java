package no.nav.veilarboppfolging.feed.cjm.common;

import no.nav.common.auth.context.AuthContextHolder;

import java.util.List;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    private final List<String> allowedUsers;

    public OidcFeedAuthorizationModule(List<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return AuthContextHolder
                .getSubject()
                .map(String::toLowerCase)
                .map(allowedUsers::contains)
                .orElse(false);
    }

}
