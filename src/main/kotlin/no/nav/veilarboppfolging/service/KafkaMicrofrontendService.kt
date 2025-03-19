package no.nav.veilarboppfolging.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service



@Slf4j
@Service
class KafkaMicrofrontendService (
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val kafkaProducerService: KafkaProducerService,
    private val leaderElectionClient: LeaderElectionClient
){

    val logger = LoggerFactory.getLogger(this::class.java)

//    @Scheduled(initialDelay = 60000, fixedDelay = 21600000)
    fun aktiverMicrofrontendForBrukereUnderOppfolging() {
        if (!leaderElectionClient.isLeader()) {
            return;
        }

        var microfrontendEntities = oppfolgingsPeriodeRepository.hentAlleSomSkalAktiveres()
        logger.info("Antall som skal aktiveres: ${microfrontendEntities.size}")

        var vellykkede = 0;
        var feilet = 0;
        var totalt = 0;

        for(microfrontendEntity in microfrontendEntities) {
            try {
                kafkaProducerService.publiserVisAoMinSideMicrofrontend(AktorId.of(microfrontendEntity.aktorId))

                oppfolgingsPeriodeRepository.aktiverMicrofrontendSuccess(AktorId.of(microfrontendEntity.aktorId))
                vellykkede++
            } catch (e: Exception) {
                oppfolgingsPeriodeRepository.aktiverMicrofrontendFailed(AktorId.of(microfrontendEntity.aktorId), e.message)
                feilet++
            }
            totalt++;

            if(totalt % 5000 == 0) {
                logger.info("Antall aktiveringer: $totalt. Vellykkede: $vellykkede. Feilet: $feilet")
            }
        }
        logger.info("Ferdig. Antall aktiveringer: $totalt. Vellykkede: $vellykkede. Feilet: $feilet")
    }

//    @Scheduled(initialDelay = 60000, fixedDelay = 21600000)
    fun deaktiverMicrofrontendForBrukereUnderOppfolging() {
        if (!leaderElectionClient.isLeader()) {
            return;
        }

        var microfrontendEntities = oppfolgingsPeriodeRepository.hentAlleSomSkalDeaktiveres()
        logger.info("Antall som skal deaktiveres: ${microfrontendEntities.size}")

        var vellykkede = 0;
        var feilet = 0;
        var totalt = 0;

        for(microfrontendEntity in microfrontendEntities) {
            try {
                kafkaProducerService.publiserSkjulAoMinSideMicrofrontend(AktorId.of(microfrontendEntity.aktorId))

                oppfolgingsPeriodeRepository.deaktiverMicrofrontendSuccess(AktorId.of(microfrontendEntity.aktorId))
                vellykkede++
            } catch (e: Exception) {
                oppfolgingsPeriodeRepository.deaktiverMicrofrontendFailed(AktorId.of(microfrontendEntity.aktorId), e.message)
                feilet++
            }
            totalt++;

            if(totalt % 5000 == 0) {
                logger.info("Antall deaktiveringer: $totalt. Vellykkede: $vellykkede. Feilet: $feilet")
            }
        }
        logger.info("Ferdig. Antall deaktiveringer: $totalt. Vellykkede: $vellykkede. Feilet: $feilet")
    }
}