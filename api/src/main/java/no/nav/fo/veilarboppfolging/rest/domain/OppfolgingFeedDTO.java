package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.sql.Timestamp;

@Value
@Builder
@Wither
public class OppfolgingFeedDTO implements Comparable<OppfolgingFeedDTO> {
    public static final String FEED_NAME = "situasjon";

    String aktoerid;
    String veileder;
    boolean oppfolging;
    Timestamp endretTimestamp;

    @Override
    public int compareTo(OppfolgingFeedDTO o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}
