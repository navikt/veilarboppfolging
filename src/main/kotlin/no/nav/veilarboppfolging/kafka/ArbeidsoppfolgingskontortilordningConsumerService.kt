package no.nav.veilarboppfolging.kafka

import no.nav.veilarboppfolging.service.OppfolgingskontorMelding
import no.nav.veilarboppfolging.service.OppfolgingsperiodeEndretService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ArbeidsoppfolgingskontortilordningConsumerService(
    val oppfolgingsperiodeEndretService: OppfolgingsperiodeEndretService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeKontortilordning(kafkaMelding: ConsumerRecord<String, OppfolgingskontorMelding?>) {
        // Hvis tombStone -> ignorer fordi vi har sendt avslutttetMelding når perioden ble avslutta
        val value = kafkaMelding.value() ?: return
        // Hvis ikke tombStone -> publiser melding med oppføgingsperiodeStart

        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(value)
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