package no.nav.veilarboppfolging.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.mappers.KvpMapper;
import no.nav.veilarboppfolging.rest.domain.KvpDTO;
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
    public Stream<FeedElement<KvpDTO>> fetchData(String lastSerialStr, int pageSize) {
        long lastSerial = Long.parseLong(lastSerialStr);

        return repository
                .serialGreaterThan(lastSerial, pageSize)
                .stream()
                .map(o -> new FeedElement<KvpDTO>()
                        .setId(Long.toString(o.getSerial()))
                        .setElement(KvpMapper.KvpToDTO(o))
                );
    }
}
