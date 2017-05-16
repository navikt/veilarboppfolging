package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.feed.producer.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
public class TilordningFeedProvider implements FeedProvider<OppfolgingBruker> {

    private BrukerRepository repository;

    @Inject
    public TilordningFeedProvider(BrukerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingBruker>> fetchData(ZonedDateTime sinceId, int pageSize) {

        Instant instant = Instant.from(sinceId);
        LocalDateTime localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        Timestamp timestamp = Timestamp.valueOf(localTime);

        LocalDateTime localDateTime = timestamp.toLocalDateTime();
        ZonedDateTime.of(localDateTime, ZoneId.systemDefault());

        return repository
                .hentTilordningerEtterTimestamp(timestamp)
                .stream()
                .map(b -> new FeedElement<OppfolgingBruker>()
                        .setId(toZonedDateTime(b.getEndretTimestamp()))
                        .setElement(b));
    }

    private ZonedDateTime toZonedDateTime(Timestamp endretTimestamp) {
        LocalDateTime localDateTime = endretTimestamp.toLocalDateTime();
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }

}
