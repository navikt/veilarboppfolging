package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusFeedItem;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.stream.Stream;

@Component
public class OppfolgingStatusFeedProvider implements FeedProvider<OppfolgingStatusFeedItem> {

    private SituasjonRepository repository;

    @Inject
    public OppfolgingStatusFeedProvider(SituasjonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingStatusFeedItem>> fetchData(String sinceId, int pageSize) {
        Date date = Date.from(DateUtils.toTimeStamp(sinceId).toInstant());

        return repository
                .hentOppfolgingStatusFeedItemsEtterDato(date)
                .stream()
                .map(o -> new FeedElement<OppfolgingStatusFeedItem>()
                        .setId(o.getAvslutningsdato().toString())
                        .setElement(o));
    }
}
