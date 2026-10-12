package no.nav.veilarboppfolging.kafka

import no.nav.veilarboppfolging.service.AapHendelseService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class AapHendelserConsumerService(
    val aapHendelserService: AapHendelseService
) {
    fun consumeAapHendelse(kafkaMelding: ConsumerRecord<String, AapHendelseRecord>) {
        aapHendelserService.håndterAapHendelse(kafkaMelding.key(), kafkaMelding.value())
    }
}

data class AapHendelseRecord(
    val ident: String,
    val hendelse: Hendelse,
)

enum class Hendelse {
    VEDTAK,
    SOKNAD,
}