package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.DateUtils;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.toZonedDateTime;

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

        List<OppfolgingFeedDTO> data;
        boolean dateId;

        try {
            Timestamp timestamp = DateUtils.toTimeStamp(sinceId);
            data = repository.hentEndringerEtterTimestamp(timestamp, pageSize);
            dateId = true;
        } catch (Exception e) {
            log.info("Id var ikke gyldig dato. Forsøker numerisk id");
            try {
                data = repository.hentEndringerEtterId(sinceId, pageSize);
                dateId = false;
            } catch (Exception e2) {
                log.info("Feil ved henting av data for id [{}]", sinceId);
                throw e2;
            }
        } 

        final boolean finalDateId = dateId;
        log.info("OppfolgingFeedProviderDebug feed-response: {}", data);
        return data
                .stream()
                .map(b -> new FeedElement<OppfolgingFeedDTO>()
                        .setId(finalDateId ? (toZonedDateTime(b.getEndretTimestamp()).toString()) : ("" + b.getFeedId()))
                        .setElement(b));
    }
}
