package no.nav.veilarboppfolging.schedule;

import no.nav.common.job.JobRunner;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdPaOppfolgingFeedSchedule {

    public static final int INSERT_ID_INTERVAL = 500;

    private final LeaderElectionClient leaderElectionClient;

    private final OppfolgingFeedRepository oppfolgingFeedRepository;

    @Autowired
    public IdPaOppfolgingFeedSchedule(LeaderElectionClient leaderElectionClient, OppfolgingFeedRepository oppfolgingFeedRepository) {
        this.leaderElectionClient = leaderElectionClient;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
    }

    @Scheduled(fixedDelay = INSERT_ID_INTERVAL)
    public void settIderPaFeedElementer() {
        if (leaderElectionClient.isLeader()) {
            JobRunner.run("oppfolging_feed_sett_ider", oppfolgingFeedRepository::insertFeedId);
        }
    }

}
