package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.mappers.KvpMapper;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Stream;

@Component
public class KvpFeedProvider implements FeedProvider<KvpDTO> {

    private KvpRepository repository;

    @Inject
    public KvpFeedProvider(KvpRepository repo) {
        this.repository = repo;
    }

    @Override
    public Stream<FeedElement<KvpDTO>> fetchData(String sinceIdStr, int pageSize) {
        long sinceId = Long.parseLong(sinceIdStr);

        return repository
                .idGreaterThan(sinceId, pageSize)
                .stream()
                .map(o -> new FeedElement<KvpDTO>()
                        .setId(Long.toString(o.getKvpId()))
                        .setElement(KvpMapper.KvpToDTO(o))
                );
    }
}
