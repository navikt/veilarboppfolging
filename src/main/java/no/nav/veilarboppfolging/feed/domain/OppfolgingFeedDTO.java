package no.nav.veilarboppfolging.feed.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Value
@Builder
@With
public class OppfolgingFeedDTO implements Comparable<OppfolgingFeedDTO> {
    public static final String FEED_NAME = "oppfolging";

    String aktoerid;
    String veileder;
    boolean oppfolging;
    boolean nyForVeileder;
    boolean manuell;
    Timestamp endretTimestamp;
    Timestamp startDato;
    BigDecimal feedId;

    @Override
    public int compareTo(OppfolgingFeedDTO o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}
