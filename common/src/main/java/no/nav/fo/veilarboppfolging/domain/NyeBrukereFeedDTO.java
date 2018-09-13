package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.fo.feed.common.FeedElement;

import java.util.Date;

@Value
@Builder
public class NyeBrukereFeedDTO implements Comparable<NyeBrukereFeedDTO>{
    private long id;
    private String aktorId;
    private String foreslattInnsatsgruppe;
    private Date opprettet;

    @Override
    public int compareTo(NyeBrukereFeedDTO o) {
        return Long.compare(id, o.id);
    }

    public static FeedElement<NyeBrukereFeedDTO> toFeedElement(NyeBrukereFeedDTO e) {
        return new FeedElement<NyeBrukereFeedDTO>().setId(Long.toString(e.getId())).setElement(e);
    }
}
