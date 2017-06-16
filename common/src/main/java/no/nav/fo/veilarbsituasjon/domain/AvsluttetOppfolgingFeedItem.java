package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Builder
public class AvsluttetOppfolgingFeedItem implements Comparable<AvsluttetOppfolgingFeedItem> {

    public String aktoerid;
    public Date sluttdato;
    public Date oppdatert;

    @Override
    public int compareTo(AvsluttetOppfolgingFeedItem avsluttetOppfolgingFeedItem) {
        return oppdatert.compareTo(avsluttetOppfolgingFeedItem.oppdatert);
    }

}
