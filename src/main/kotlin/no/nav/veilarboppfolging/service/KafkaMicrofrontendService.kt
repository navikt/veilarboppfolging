package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KafkaMicrofrontendService (
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val kafkaProducerService: KafkaProducerService
){

    @Scheduled(cron = "0 24 13 * * *")
    fun aktiverMicrofrontendForBrukereUnderOppfolging() {

        var microfrontendEntities = oppfolgingsPeriodeRepository.hentAlleSomSkalAktiveres()

        var utvalgteEntities = microfrontendEntities.take(10)

        for(microfrontendEntity in utvalgteEntities) {
            kafkaProducerService.publiserVisAoMinSideMicrofrontend(AktorId.of(microfrontendEntity.aktorId))

            oppfolgingsPeriodeRepository.aktiverMicrofrontend(AktorId.of(microfrontendEntity.aktorId))
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