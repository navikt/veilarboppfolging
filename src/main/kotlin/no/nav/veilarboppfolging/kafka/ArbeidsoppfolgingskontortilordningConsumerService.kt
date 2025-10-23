package no.nav.veilarboppfolging.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.*

class ArbeidsoppfolgingskontortilordningConsumerService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeKontortilordning(kafkaMelding: ConsumerRecord<String, OppfolgingskontorMelding?>) {
        logger.info("Kom inn til funksjon for konsumering av melding")
    }
}

data class OppfolgingskontorMelding(
    val kontorId: String,
    val kontorNavn: String,
    val oppfolgingsperiodeId: UUID,
    val aktorId: String,
    val ident: String
)