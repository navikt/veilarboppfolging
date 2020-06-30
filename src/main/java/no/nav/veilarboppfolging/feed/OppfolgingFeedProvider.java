package no.nav.veilarboppfolging.feed;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class OppfolgingFeedProvider implements FeedProvider<OppfolgingFeedDTO> {

    private OppfolgingFeedRepository repository;

    @Inject
    public OppfolgingFeedProvider(OppfolgingFeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        log.info("OppfolgingFeedProviderDebug requested sinceId: {}", sinceId);

        List<OppfolgingFeedDTO> oppfolgingStatuser;

        oppfolgingStatuser = repository.hentEndringerEtterId(sinceId, pageSize);

        log.info("OppfolgingFeedProviderDebug: {} oppfolgingsfeed dtoer fra databasen", oppfolgingStatuser.size());
        log.info("OppfolgingFeedProviderDebug: {}", oppfolgingStatuser);

        return oppfolgingStatuser
                .stream()
                .map(oppfolgingstatus -> new FeedElement<OppfolgingFeedDTO>()
                        .setId(oppfolgingstatus.getFeedId().toString())
                        .setElement(oppfolgingstatus));
    }
}
