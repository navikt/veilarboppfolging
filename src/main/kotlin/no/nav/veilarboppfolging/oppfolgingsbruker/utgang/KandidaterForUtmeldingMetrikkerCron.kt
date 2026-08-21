package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.KandidaterForUtmeldingMetrikker
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/** Samler og sender metrikker for kandidater_for_utmelding til BigQuery. */
@Service
class KandidaterForUtmeldingMetrikkerCron(
    private val bigQueryClient: BigQueryClient,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val leaderElectionClient: LeaderElectionClient,
) {

    @Scheduled(cron = "0 0 0 * * *")
    fun målOgLoggKandidaterForUtmeldingMetrikker() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("kandidater_for_utmelding_metrikker", UnsafeRunnable {
            loggKandidaterForUtmeldingMetrikker()
        })
    }

    fun loggKandidaterForUtmeldingMetrikker() {
        val antallKandidaterForUtmelding = kandidatForUtmeldingRepository.hentAntallKandidaterForUtmelding()
        val antallUnderOppfolgingMedIserv = oppfolgingsStatusRepository.hentAntallUnderOppfolgingMedIserv()
        val antallKandidaterForUtmeldingForlenget = kandidatForUtmeldingRepository.hentAntallKandidaterForUtmeldingForlenget()
        val metrikker = KandidaterForUtmeldingMetrikker(
            antallKandidaterForUtmelding = antallKandidaterForUtmelding,
            antallUnderOppfolgingMedIserv = antallUnderOppfolgingMedIserv,
            antallKandidaterForUtmeldingForlenget = antallKandidaterForUtmeldingForlenget,
        )
        bigQueryClient.loggKandidaterForUtmeldingMetrikker(metrikker)
    }
}
