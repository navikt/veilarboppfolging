package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class OppfolgingStatusFeedItem implements Comparable<OppfolgingStatusFeedItem> {

    private String aktoerid;
    private String veilederid;
    private Boolean oppfolging;
    private Date avslutningsdato;

    @Override
    public int compareTo(OppfolgingStatusFeedItem oppfolgingStatusFeedItem) {
        return avslutningsdato.compareTo(oppfolgingStatusFeedItem.avslutningsdato);
    }

}
