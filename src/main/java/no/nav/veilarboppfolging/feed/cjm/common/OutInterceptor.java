package no.nav.veilarboppfolging.feed.cjm.common;

import okhttp3.Request;

public interface OutInterceptor {
    void apply(Request.Builder builder);
}
