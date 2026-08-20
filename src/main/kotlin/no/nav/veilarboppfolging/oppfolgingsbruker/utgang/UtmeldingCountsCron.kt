package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.UtmeldingsAntall
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/** Samler og sender utmeldingsmetrikker til BigQuery. */
@Service
class UtmeldingCountsCron(
    private val bigQueryClient: BigQueryClient,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val leaderElectionClient: LeaderElectionClient,
) {

    @Scheduled(cron = "0 0 * * * *")
    fun målOgLoggUtmeldingCounts() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("utmelding_counts", UnsafeRunnable {
            loggUtmeldingCounts()
        })
    }

    fun loggUtmeldingCounts() {
        val antallKandidaterForUtmelding = kandidatForUtmeldingRepository.hentAntallKandidaterForUtmelding()
        val antallUnderOppfolgingMedIserv = oppfolgingsStatusRepository.hentAntallUnderOppfolgingMedIserv()
        val utmeldingsAntall = UtmeldingsAntall(
            personerIUtemelding = antallKandidaterForUtmelding,
            personIUtmeldingSomErUnderOppfolgingOgIserv = antallUnderOppfolgingMedIserv,
        )
        bigQueryClient.loggUtmeldingsCount(utmeldingsAntall)
    }
}
