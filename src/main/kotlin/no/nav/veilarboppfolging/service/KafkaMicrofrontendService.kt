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

    @Scheduled(cron = "0 54 13 * * *")
    fun aktiverMicrofrontendForBrukereUnderOppfolging() {

        var microfrontendEntities = oppfolgingsPeriodeRepository.hentAlleSomSkalAktiveres()

        var utvalgteEntities = microfrontendEntities.takeLast(10)

        for(microfrontendEntity in utvalgteEntities) {
            try {
                kafkaProducerService.publiserVisAoMinSideMicrofrontend(AktorId.of(microfrontendEntity.aktorId))

                oppfolgingsPeriodeRepository.aktiverMicrofrontend(AktorId.of(microfrontendEntity.aktorId))
            } catch (e: Exception) {
                logger.warn("Dette gikk dårlig gitt: ${microfrontendEntity.aktorId}. Exception: ", e.message )
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