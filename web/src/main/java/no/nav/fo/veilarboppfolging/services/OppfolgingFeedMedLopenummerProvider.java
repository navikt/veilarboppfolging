package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class OppfolgingFeedMedLopenummerProvider implements FeedProvider<OppfolgingFeedDTO> {

    private OppfolgingFeedRepository repository;

    @Inject
    public OppfolgingFeedMedLopenummerProvider(OppfolgingFeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<FeedElement<OppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {

        log.info("Henter elementer fom id [{}]", sinceId);

        List<OppfolgingFeedDTO> data = repository.hentEndringerEtterId(sinceId, pageSize);

        return data
                .stream()
                .map(b -> new FeedElement<OppfolgingFeedDTO>()
                        .setId("" + b.getFeedId())
                        .setElement(b));
    }
}
