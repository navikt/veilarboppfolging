package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;
import java.util.Date;

@Value
@Builder
public class AvsluttOppfolgingFeedItem implements Comparable<AvsluttOppfolgingFeedItem> {

    private String aktoerid;
    private Date sluttdato;
    private Timestamp oppdatert;

    @Override
    public int compareTo(AvsluttOppfolgingFeedItem avsluttOppfolgingFeedItem) {
        return oppdatert.compareTo(avsluttOppfolgingFeedItem.oppdatert);
    }

}
