package no.nav.veilarboppfolging.feed.cjm.common;

import no.nav.common.sts.SystemUserTokenProvider;
import okhttp3.Request;

public class OidcFeedOutInterceptor implements OutInterceptor {

    private final SystemUserTokenProvider systemUserTokenProvider;

    public OidcFeedOutInterceptor(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    @Override
    public void apply(Request.Builder builder) {
        builder.header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken());
    }

}
