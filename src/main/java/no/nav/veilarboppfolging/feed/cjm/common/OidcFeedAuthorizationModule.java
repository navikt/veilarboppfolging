package no.nav.veilarboppfolging.feed.cjm.common;

import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;

import java.util.List;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    private final List<String> allowedUsers;

    public OidcFeedAuthorizationModule(List<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return SubjectHandler.getSubject()
                .map(Subject::getUid)
                .map(String::toLowerCase)
                .map(allowedUsers::contains)
                .orElse(false);
    }

}
