package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbsituasjon.domain.AvsluttetOppfolgingFeedData;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
public class AvsluttetOppfolgingFeedProvider implements FeedProvider<AvsluttetOppfolgingFeedDTO> {

    private SituasjonOversiktService situasjonOversiktService;

    @Inject
    public AvsluttetOppfolgingFeedProvider(SituasjonOversiktService situasjonOversiktService) {
        this.situasjonOversiktService = situasjonOversiktService;
    }

    @Override
    public Stream<FeedElement<AvsluttetOppfolgingFeedDTO>> fetchData(String sinceId, int pageSize) {
        Timestamp timestamp = DateUtils.toTimeStamp(sinceId);

        return situasjonOversiktService
                .hentAvsluttetOppfolgingEtterDato(timestamp)
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
