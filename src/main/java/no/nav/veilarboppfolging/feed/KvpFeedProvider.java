package no.nav.veilarboppfolging.feed;

import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.feed.cjm.common.FeedElement;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProvider;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class KvpFeedProvider implements FeedProvider<KvpDTO> {

    private final KvpRepository repository;

    @Autowired
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
                        .setElement(DtoMappers.kvpToDTO(o))
                );
    }
}
