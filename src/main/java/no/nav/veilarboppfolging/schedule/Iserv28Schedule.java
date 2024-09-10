package no.nav.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarboppfolging.service.IservService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Iserv28Schedule {

    private final LeaderElectionClient leaderElectionClient;

    private final IservService iservService;

    @Autowired
    public Iserv28Schedule(LeaderElectionClient leaderElectionClient, IservService iservService) {
        this.leaderElectionClient = leaderElectionClient;
        this.iservService = iservService;
    }

    // TODO uncomment when ready to gcp
    //@Scheduled(cron="0 0 * * * *")
    public void scheduledAvslutteOppfolging() {
        if (leaderElectionClient.isLeader()) {
            JobRunner.run("iserv28_avslutt_oppfolging", iservService::automatiskAvslutteOppfolging);
        }
    }

}
