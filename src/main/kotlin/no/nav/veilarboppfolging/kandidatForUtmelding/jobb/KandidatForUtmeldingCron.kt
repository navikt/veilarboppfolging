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
    @Scheduled(cron = "0 10 * * * *")
    fun behandleKandidaterMedUtloptForlengelse() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("behandle_kandidater_utlopt_forlengelse") {
            kandidatForUtmeldingService.behandleKandidaterMedUtloptForlengelse()
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    fun avsluttOppfolgingForKandidaterMedPassertAvsluttesAutomatiskDato() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("avslutt_oppfolging_for_kandidater_med_passert_avsluttes_automatisk_dato") {
            kandidatForUtmeldingService.avsluttOppfolgingForKandidaterMedPassertAvsluttesAutomatiskDato()
        }
    }

    @Scheduled(cron = "0 20 * * * *")
    fun fjernKandidaterSomIkkeKanAvsluttesManuelt() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("fjern_kandidater_som_ikke_kan_avsluttes_manuelt") {
            kandidatForUtmeldingService.fjernKandidaterSomIkkeKanAvsluttesManuelt()
        }
    }

    // midlertidig jobb som skal fjerne radene som ligger i kandidater_som_ikke_kan_avsluttes hvis årsaken er hendelser som vi håndterer nå,
    // eller oppdatere dem med eventuelt flere årsaker hvis de mangler årsaker til at de ikke kan avsluttes
    @Scheduled(cron = "0 30 * * * *")
    fun oppdaterEllerSlettKandidaterSomIkkeKanAvsluttes() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("oppdater_eller_slett_kandidater_som_ikke_kan_avsluttes") {
            kandidatForUtmeldingService.oppdaterEllerSlettKandidaterSomIkkeKanAvsluttes()
        }
    }
}