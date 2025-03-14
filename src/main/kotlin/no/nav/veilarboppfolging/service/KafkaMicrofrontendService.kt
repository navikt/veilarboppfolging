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

    @Scheduled(cron = "0 15 12 * * *")
    fun aktiverMicrofrontendForBrukereUnderOppfolging() {

        var oppfolgingsperioder = oppfolgingsPeriodeRepository.hentAlleAktiveOppfolgingsperioder()
        oppfolgingsPeriodeRepository.insertAlleAktiveOppfolgingsperioder(oppfolgingsperioder);

        for(oppfolgingsperiode in oppfolgingsperioder) {
            kafkaProducerService.publiserVisAoMinSideMicrofrontend(AktorId.of(oppfolgingsperiode.aktorId))

            oppfolgingsPeriodeRepository.aktiverMicrofrontend(AktorId.of(oppfolgingsperiode.aktorId))
        }
    }

//    @Scheduled(cron = "0 0 * * * *") // Hver time
    fun deaktiverMicrofrontendForBrukereSomAvsluttetOppfolgingIDag() {

        var oppfolgingsperioder = oppfolgingsPeriodeRepository.hentAlleAktiveOppfolgingsperioder()
        oppfolgingsPeriodeRepository.insertAlleIkkeAktiveOppfolgingsperioder(oppfolgingsperioder);


    }
}