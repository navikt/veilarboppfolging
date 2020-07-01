package no.nav.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarboppfolging.services.IservService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Iserv28Schedule {

    private final LeaderElectionClient leaderElectionClient;

    private final IservService iservService;

    public Iserv28Schedule(LeaderElectionClient leaderElectionClient, IservService iservService) {
        this.leaderElectionClient = leaderElectionClient;
        this.iservService = iservService;
    }

    @Scheduled(cron="0 0 * * * *")
    public void scheduledAvslutteOppfolging() {
        if (leaderElectionClient.isLeader()) {
            iservService.automatiskAvslutteOppfolging();
        }
    }

}
