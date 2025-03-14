package no.nav.veilarboppfolging.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service



@Slf4j
@Service
class KafkaMicrofrontendService (
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val kafkaProducerService: KafkaProducerService,
){

    val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 45 14 * * *")
    fun aktiverMicrofrontendForBrukereUnderOppfolging() {

        var microfrontendEntities = oppfolgingsPeriodeRepository.hentAlleSomSkalAktiveres()

        logger.info("Fant ${microfrontendEntities.count()} brukere som skal aktiveres.")

        var utvalgteEntities = microfrontendEntities.takeLast(10)

        for(microfrontendEntity in utvalgteEntities) {
            try {
                kafkaProducerService.publiserVisAoMinSideMicrofrontend(AktorId.of(microfrontendEntity.aktorId))
                logger.info("Har sendt aktiver-melding for ${microfrontendEntity.aktorId}")

                oppfolgingsPeriodeRepository.aktiverMicrofrontend(AktorId.of(microfrontendEntity.aktorId))
                logger.info("Har lagret aktiver-status for ${microfrontendEntity.aktorId}")
            } catch (e: Exception) {
                logger.warn("Dette gikk dårlig gitt: ${microfrontendEntity.aktorId}. Exception: ${e.message}")
            }
        }
    }

//    @Scheduled(cron = "0 0 * * * *") // Hver time
//    fun deaktiverMicrofrontendForBrukereSomAvsluttetOppfolgingIDag() {
//
//        var oppfolgingsperioder = oppfolgingsPeriodeRepository.hentAlleAktiveOppfolgingsperioder()
//        oppfolgingsPeriodeRepository.insertAlleIkkeAktiveOppfolgingsperioder(oppfolgingsperioder);
//
//
//    }
}