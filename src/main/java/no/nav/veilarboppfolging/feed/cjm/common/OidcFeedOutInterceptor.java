package no.nav.veilarboppfolging.feed.cjm.common;

import no.nav.common.sts.SystemUserTokenProvider;

import javax.ws.rs.client.Invocation;

public class OidcFeedOutInterceptor implements OutInterceptor {

    private final SystemUserTokenProvider systemUserTokenProvider;

    public OidcFeedOutInterceptor(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    @Override
    public void apply(Invocation.Builder builder) {
        builder.header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken());
    }

}
