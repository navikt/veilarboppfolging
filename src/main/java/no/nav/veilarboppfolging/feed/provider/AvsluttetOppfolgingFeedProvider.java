package no.nav.veilarboppfolging.feed.provider;

import no.nav.veilarboppfolging.feed.cjm.common.FeedElement;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProvider;
import no.nav.veilarboppfolging.feed.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarboppfolging.repository.entity.AvsluttetOppfolgingFeedEntity;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
public class AvsluttetOppfolgingFeedProvider implements FeedProvider<AvsluttetOppfolgingFeedDTO> {

    private final OppfolgingService oppfolgingService;

    @Autowired
    public AvsluttetOppfolgingFeedProvider(OppfolgingService oppfolgingService) {
        this.oppfolgingService = oppfolgingService;
    }

    @Override
    public Stream<FeedElement<AvsluttetOppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = Timestamp.from(ZonedDateTime.parse(sinceId).toInstant());

        return oppfolgingService
                .hentAvsluttetOppfolgingEtterDato(timestamp, pageSize)
                .stream()
                .map(o -> new FeedElement<AvsluttetOppfolgingFeedDTO>()
                        .setId(ZonedDateTime.ofInstant(o.getOppdatert().toInstant(), ZoneId.systemDefault()).toString())
                        .setElement(tilDTO(o))
                );
    }

    private AvsluttetOppfolgingFeedDTO tilDTO(AvsluttetOppfolgingFeedEntity avsluttetOppfolgingFeedData) {
        return new AvsluttetOppfolgingFeedDTO()
                .setAktoerid(avsluttetOppfolgingFeedData.getAktoerid())
                .setOppdatert(avsluttetOppfolgingFeedData.getOppdatert())
                .setSluttdato(avsluttetOppfolgingFeedData.getSluttdato());
    }
}
