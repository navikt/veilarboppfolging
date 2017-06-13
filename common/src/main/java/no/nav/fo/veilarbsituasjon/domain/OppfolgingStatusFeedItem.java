package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Date;

@Value
@Builder
@Accessors(chain = true)
public class OppfolgingStatusFeedItem implements Comparable<OppfolgingStatusFeedItem> {

    private String aktoerid;
    private Boolean oppfolging;
    private Date avslutningsdato;

    @Override
    public int compareTo(OppfolgingStatusFeedItem oppfolgingStatusFeedItem) {
        return avslutningsdato.compareTo(oppfolgingStatusFeedItem.avslutningsdato);
    }

}
