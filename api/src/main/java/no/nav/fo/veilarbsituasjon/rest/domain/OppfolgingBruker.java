package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.sql.Timestamp;

@Value
@Builder
@Wither
public class OppfolgingBruker implements Comparable<OppfolgingBruker> {

    public static final String FEED_NAME = "situasjon";

    String aktoerid;
    String veileder;
    boolean oppfolging;
    Timestamp endretTimestamp;

    @Override
    public int compareTo(OppfolgingBruker o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}
