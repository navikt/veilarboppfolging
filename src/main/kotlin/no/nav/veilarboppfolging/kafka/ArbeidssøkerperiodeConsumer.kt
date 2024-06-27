package no.nav.veilarboppfolging.kafka

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidssøkerperiodeConsumer {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeArbeidssøkerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        logger.info("Hopper over melding fra nytt arbeidssøkerregister")
        TODO("Not implemented")
    }
}
