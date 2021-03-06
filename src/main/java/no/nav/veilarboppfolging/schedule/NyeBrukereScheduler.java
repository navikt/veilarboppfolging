package no.nav.veilarboppfolging.schedule;

import no.nav.common.job.JobRunner;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarboppfolging.repository.NyeBrukereFeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class NyeBrukereScheduler {

    private final LeaderElectionClient leaderElectionClient;

    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @Autowired
    public NyeBrukereScheduler(LeaderElectionClient leaderElectionClient, NyeBrukereFeedRepository nyeBrukereFeedRepository) {
        this.leaderElectionClient = leaderElectionClient;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
    }

    @Scheduled(fixedDelayString = "${nyebrukere.oppdater.id.rate:10000}")
    public void settIdeerPaFeedElementer() {
        if (leaderElectionClient.isLeader()) {
            JobRunner.run("nye_brukere_feed_sett_ider", nyeBrukereFeedRepository::leggTilFeedIdPaAlleElementerUtenFeedId);
        }
    }
}
