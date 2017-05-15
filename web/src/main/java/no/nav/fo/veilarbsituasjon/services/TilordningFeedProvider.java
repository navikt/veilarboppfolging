package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.feed.producer.FeedElement;
import no.nav.fo.veilarbsituasjon.rest.feed.producer.FeedProvider;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class TilordningFeedProvider implements FeedProvider<OppfolgingBruker, LocalDateTime> {

    private BrukerRepository repository;

    @Inject
    public TilordningFeedProvider(BrukerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<FeedElement<OppfolgingBruker, LocalDateTime>> hentData(LocalDateTime sinceId, int pageSize) {
        Timestamp timestamp = Timestamp.valueOf(sinceId);

        return repository
                .hentTilordningerEtterTimestamp(timestamp)
                .stream()
                .map(b -> new FeedElement<OppfolgingBruker, LocalDateTime>().setId(b.getEndretTimestamp().toLocalDateTime()).setElement(b))
                .collect(toList());
    }
}
