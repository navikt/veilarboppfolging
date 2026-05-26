package no.nav.veilarboppfolging.kafka

import java.util.UUID
import no.nav.veilarboppfolging.service.ArbeidsoppfolgingsKontorEndretService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class ArbeidsoppfolgingskontortilordningConsumerService(
    val arbeidsoppfolgingsKontorEndretService: ArbeidsoppfolgingsKontorEndretService
) {
    fun consumeKontortilordning(kafkaMelding: ConsumerRecord<Long, OppfolgingskontorMelding?>) {
        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(kafkaMelding.key(), kafkaMelding.value())
    }
}

data class OppfolgingskontorMelding(
    val kontorId: String,
    val kontorNavn: String,
    val oppfolgingsperiodeId: UUID,
    val aktorId: String,
    val ident: String,
    val tilordningstype: Tilordningstype
)

enum class Tilordningstype {
    KONTOR_VED_OPPFOLGINGSPERIODE_START,
    ENDRET_KONTOR;
}