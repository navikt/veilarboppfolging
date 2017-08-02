package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class OppfolgingBruker implements Comparable<OppfolgingBruker> {

    public static final String FEED_NAME = "situasjon";

    String aktoerid;
    String veileder;
    Boolean oppfolging;
    Timestamp endretTimestamp;

    @Override
    public int compareTo(OppfolgingBruker o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}
