package no.nav.veilarboppfolging.feed.cjm.common;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class FeedRequest {
    String sinceId;
    int pageSize;
}

