package no.nav.veilarboppfolging.service

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MigrerAvsluttetAvTypeCron(
    private val leaderElectionClient: LeaderElectionClient,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *")
    fun migrerAvsluttetAvType() {
        if(!leaderElectionClient.isLeader) return
        JobRunner.run("migrer_avsluttet_av_type_system") {
            val antall = oppfolgingsPeriodeRepository.settAvsluttetAvTypeTilSystemForAvsluttedePerioder(50_000)
            logger.info("Migrerte $antall oppfølgingsperioder til avsluttet_av_type SYSTEM")
        }
    }
}