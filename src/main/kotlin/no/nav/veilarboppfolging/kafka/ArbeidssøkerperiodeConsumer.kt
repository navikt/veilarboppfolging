package no.nav.veilarboppfolging.kafka

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class ArbeidssøkerperiodeConsumer {

    fun consumeArbeidssøkerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        TODO("Not implemented - expect test to fail")
    }
}
