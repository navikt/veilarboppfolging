package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarboppfolging.domain.AvsluttetOppfolgingFeedData;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
public class AvsluttetOppfolgingFeedProvider implements FeedProvider<AvsluttetOppfolgingFeedDTO> {

    private OppfolgingService oppfolgingService;

    @Inject
    public AvsluttetOppfolgingFeedProvider(OppfolgingService oppfolgingService) {
        this.oppfolgingService = oppfolgingService;
    }

    @Override
    public Stream<FeedElement<AvsluttetOppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = DateUtils.toTimeStamp(sinceId);

        return oppfolgingService
                .hentAvsluttetOppfolgingEtterDato(timestamp, pageSize)
                .stream()
                .map(o -> new FeedElement<AvsluttetOppfolgingFeedDTO>()
                        .setId(ZonedDateTime.ofInstant(o.oppdatert.toInstant(), ZoneId.systemDefault()).toString())
                        .setElement(tilDTO(o))
                );
    }

    private AvsluttetOppfolgingFeedDTO tilDTO(AvsluttetOppfolgingFeedData avsluttetOppfolgingFeedData) {
        return new AvsluttetOppfolgingFeedDTO()
                .setAktoerid(avsluttetOppfolgingFeedData.aktoerid)
                .setOppdatert(avsluttetOppfolgingFeedData.oppdatert)
                .setSluttdato(avsluttetOppfolgingFeedData.sluttdato);
    }
}