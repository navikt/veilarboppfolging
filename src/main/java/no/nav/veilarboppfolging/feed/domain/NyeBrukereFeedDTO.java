package no.nav.veilarboppfolging.feed.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.feed.cjm.common.FeedElement;

import java.time.ZonedDateTime;

@Value
@Builder
public class NyeBrukereFeedDTO implements Comparable<NyeBrukereFeedDTO>{
    private long id;
    private String aktorId;
    private String foreslattInnsatsgruppe;
    private String sykmeldtBrukerType;
    private ZonedDateTime opprettet;

    @Override
    public int compareTo(NyeBrukereFeedDTO o) {
        return Long.compare(id, o.id);
    }

    public static FeedElement<NyeBrukereFeedDTO> toFeedElement(NyeBrukereFeedDTO e) {
        return new FeedElement<NyeBrukereFeedDTO>().setId(Long.toString(e.getId())).setElement(e);
    }
}
