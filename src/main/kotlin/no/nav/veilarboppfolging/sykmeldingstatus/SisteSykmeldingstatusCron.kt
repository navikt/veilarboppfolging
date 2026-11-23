package no.nav.veilarboppfolging.sykmeldingstatus

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SisteSykmeldingstatusCron(
    private val leaderElectionClient: LeaderElectionClient,
    private val sisteSykmeldingstatusService: SisteSykmeldingstatusService,
) {
    @Scheduled(cron = "0 40 * * * *")
    fun opprettKandidaterForUtmeldingIkkeLengerSykmeldt() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("opprett_kandidater_for_utmelding_ikke_lenger_sykmeldt") {
            sisteSykmeldingstatusService.opprettKandidaterForUtmelding()
        }
    }
}