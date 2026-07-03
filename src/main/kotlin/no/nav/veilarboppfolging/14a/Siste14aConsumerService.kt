package no.nav.veilarboppfolging.`14a`

import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class Siste14aConsumerService(
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
) {

    fun consumeSiste14AVedtak(siste14aVedtakMelding: ConsumerRecord<String, Siste14aVedtakKafkaDto>) {
        val (aktorId, innsatsgruppe) = siste14aVedtakMelding.value()
        oppfolgingsStatusRepository.oppdaterInnsatsgruppe(aktorId, innsatsgruppe)
    }

}
