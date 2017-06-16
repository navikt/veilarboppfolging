package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.AvsluttetOppfolgingFeedItem;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.stream.Stream;

import static no.nav.fo.veilarbsituasjon.utils.DateUtils.toZonedDateTime;

@Component
public class AvsluttetOppfolgingFeedProvider implements FeedProvider<AvsluttetOppfolgingFeedItem> {

    private SituasjonRepository repository;

    @Inject
    public AvsluttetOppfolgingFeedProvider(SituasjonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<AvsluttetOppfolgingFeedItem>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = DateUtils.toTimeStamp(sinceId);

        return repository
                .hentAvsluttetOppfolgingEtterDato(timestamp)
                .stream()
                .map(o -> new FeedElement<AvsluttetOppfolgingFeedItem>()
                        .setId(toZonedDateTime(o.getOppdatert()).toString())
                        .setElement(o));
    }
}
