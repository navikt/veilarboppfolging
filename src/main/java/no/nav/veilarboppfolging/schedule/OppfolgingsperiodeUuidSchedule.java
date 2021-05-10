package no.nav.veilarboppfolging.schedule;

import lombok.RequiredArgsConstructor;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OppfolgingsperiodeUuidSchedule {

    private final static long TEN_SECONDS = 10 * 1000;

    private final LeaderElectionClient leaderElectionClient;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Scheduled(initialDelay = TEN_SECONDS, fixedRate = 1000)
    public void settUuidPaOppfolgingsperioder() {
        if (leaderElectionClient.isLeader()) {
            List<String> oppfolgingsperiodeRowIds = oppfolgingsPeriodeRepository.hentOppfolgingsPeriodeRowIdUtenUuid();
            oppfolgingsperiodeRowIds.forEach(oppfolgingsPeriodeRepository::initialiserUuidPaOppfolgingsperiode);
        }
    }

}
