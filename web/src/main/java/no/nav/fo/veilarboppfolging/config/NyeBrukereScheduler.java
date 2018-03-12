package no.nav.fo.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class NyeBrukereScheduler {

    private NyeBrukereFeedRepository nyeBrukereFeedRepository;

    public NyeBrukereScheduler(NyeBrukereFeedRepository nyeBrukereFeedRepository) {
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
    }

    @Scheduled(fixedDelayString = "${nyebrukere.oppdater.id.rate:10000}")
    public void settIdeerPaFeedElementer() {
        nyeBrukereFeedRepository.leggTilFeedIdPaAlleElementerUtenFeedId();
    }
}
