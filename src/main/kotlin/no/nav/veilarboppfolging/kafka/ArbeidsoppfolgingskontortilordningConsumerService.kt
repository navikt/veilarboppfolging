package no.nav.veilarboppfolging.kafka

import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.service.OppfolgingsperiodeEndretService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrElse

@Service
class ArbeidsoppfolgingskontortilordningConsumerService(
    val oppfolgingsperiodeEndretService: OppfolgingsperiodeEndretService
) {
    fun consumeKontortilordning(kafkaMelding: ConsumerRecord<String, OppfolgingskontorMelding?>) {
        if (EnvironmentUtils.isProduction().getOrElse { true }) {
            // Hopper over melding i prod foreløpig
        } else {
            oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(kafkaMelding.key(), kafkaMelding.value())
        }
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