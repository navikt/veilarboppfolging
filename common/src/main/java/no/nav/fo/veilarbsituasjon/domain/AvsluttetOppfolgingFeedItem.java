package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;
import java.util.Date;

@Value
@Builder
public class AvsluttetOppfolgingFeedItem implements Comparable<AvsluttetOppfolgingFeedItem> {

    private String aktoerid;
    private Date sluttdato;
    private Timestamp oppdatert;

    @Override
    public int compareTo(AvsluttetOppfolgingFeedItem avsluttetOppfolgingFeedItem) {
        return oppdatert.compareTo(avsluttetOppfolgingFeedItem.oppdatert);
    }

}
