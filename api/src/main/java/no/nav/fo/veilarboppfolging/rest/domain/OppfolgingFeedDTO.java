package no.nav.fo.veilarboppfolging.rest.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

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
    boolean manuellBruker;    //TODO Slett når FO-123 er i prod
    boolean manuell;
    Timestamp endretTimestamp;

    @Override
    public int compareTo(OppfolgingFeedDTO o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}
