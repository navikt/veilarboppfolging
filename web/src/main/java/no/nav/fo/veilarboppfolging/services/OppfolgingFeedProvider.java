package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.stream.Stream;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.toZonedDateTime;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class OppfolgingFeedProvider implements FeedProvider<OppfolgingFeedDTO> {

    private OppfolgingFeedRepository repository;
    private static final Logger LOG = getLogger(OppfolgingFeedProvider.class);

    @Inject
    public OppfolgingFeedProvider(OppfolgingFeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = DateUtils.toTimeStamp(sinceId);
        LOG.info("Henter oppfølgingfeed fra: {}", sinceId);
        return repository
                .hentTilordningerEtterTimestamp(timestamp, pageSize)
                .stream()
                .map(b -> new FeedElement<OppfolgingFeedDTO>()
                        .setId(toZonedDateTime(b.getEndretTimestamp()).toString())
                        .setElement(b));
    }
}
