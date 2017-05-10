package no.nav.fo.veilarbsituasjon.rest.feed;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

public class FeedRequest {

    @QueryParam("since_id")
    String sinceId;

    @QueryParam("page_size")
    @DefaultValue("100")
    int pageSize;
}

