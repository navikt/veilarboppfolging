package no.nav.veilarboppfolging.feed.cjm.common;

import javax.ws.rs.client.Invocation;

public interface OutInterceptor {
    void apply(Invocation.Builder builder);
}
