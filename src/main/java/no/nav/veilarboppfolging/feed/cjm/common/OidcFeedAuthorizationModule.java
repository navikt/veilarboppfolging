package no.nav.veilarboppfolging.feed.cjm.common;

import no.nav.common.auth.context.AuthContextHolder;

import java.util.List;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    private final AuthContextHolder authContextHolder;

    private final List<String> allowedUsers;

    public OidcFeedAuthorizationModule(AuthContextHolder authContextHolder, List<String> allowedUsers) {
        this.authContextHolder = authContextHolder;
        this.allowedUsers = allowedUsers;
    }

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return authContextHolder
                .getSubject()
                .map(String::toLowerCase)
                .map(allowedUsers::contains)
                .orElse(false);
    }

}
