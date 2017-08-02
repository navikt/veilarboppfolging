package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.stream.Stream;

import static no.nav.fo.veilarbsituasjon.utils.DateUtils.toZonedDateTime;

@Component
public class SituasjonFeedProvider implements FeedProvider<OppfolgingBruker> {

    private BrukerRepository repository;

    @Inject
    public SituasjonFeedProvider(BrukerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingBruker>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = DateUtils.toTimeStamp(sinceId);

        return repository
                .hentTilordningerEtterTimestamp(timestamp)
                .stream()
                .map(b -> new FeedElement<OppfolgingBruker>()
                        .setId(toZonedDateTime(b.getEndretTimestamp()).toString())
                        .setElement(b));
    }
}
