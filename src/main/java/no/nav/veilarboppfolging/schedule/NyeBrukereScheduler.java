package no.nav.veilarboppfolging.schedule;

import no.nav.veilarboppfolging.db.NyeBrukereFeedRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
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
