package no.nav.veilarboppfolging.feed;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.feed.cjm.common.FeedElement;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProvider;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class OppfolgingFeedProvider implements FeedProvider<OppfolgingFeedDTO> {

    private final OppfolgingFeedRepository repository;

    @Autowired
    public OppfolgingFeedProvider(OppfolgingFeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        log.info("OppfolgingFeedProviderDebug requested sinceId: {}", sinceId);

        List<OppfolgingFeedDTO> oppfolgingStatuser = repository.hentEndringerEtterId(sinceId, pageSize);

        log.info("OppfolgingFeedProviderDebug: {} oppfolgingsfeed dtoer fra databasen", oppfolgingStatuser.size());
        log.info("OppfolgingFeedProviderDebug: {}", oppfolgingStatuser);

        return oppfolgingStatuser
                .stream()
                .map(oppfolgingstatus -> new FeedElement<OppfolgingFeedDTO>()
                        .setId(oppfolgingstatus.getFeedId().toString())
                        .setElement(oppfolgingstatus));
    }
}
