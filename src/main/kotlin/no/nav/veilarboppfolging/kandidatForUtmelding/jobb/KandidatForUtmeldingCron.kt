package no.nav.veilarboppfolging.kandidatForUtmelding.jobb

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KandidatForUtmeldingCron(
    private val leaderElectionClient: LeaderElectionClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
) {
    @Scheduled(cron = "0 30 * * * *")
    fun behandleKandidaterMedUtloptForlengelse() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("behandle_kandidater_utlopt_forlengelse") {
            kandidatForUtmeldingService.behandleKandidaterMedUtloptForlengelse()
        }
    }
}