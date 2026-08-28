package no.nav.veilarboppfolging.kandidatForUtmelding.jobb

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KandidatForUtmeldingCron(
    private val leaderElectionClient: LeaderElectionClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
    private val avsluttOppfolgingService: AvsluttOppfolgingService
) {
    @Scheduled(cron = "0 0 5 * * *")
    fun behandleKandidaterMedUtloptForlengelse() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("behandle_kandidater_utlopt_forlengelse") {
            kandidatForUtmeldingService.behandleKandidaterMedUtloptForlengelse()
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    fun avsluttOppfolgingForKandidaterSomSkalAutomatiskAvsluttes() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("avslutt_oppfolging_for_kandidater_som_skal_automatisk_avsluttes") {
            avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes()
        }
    }
}