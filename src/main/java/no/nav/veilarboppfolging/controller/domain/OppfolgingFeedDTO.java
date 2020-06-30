package no.nav.veilarboppfolging.controller.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Value
@Builder
@Wither
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