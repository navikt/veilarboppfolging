package no.nav.veilarboppfolging.`14a`

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class Siste14aConsumerService {

    fun consumeSiste14AVedtak(siste14aVedtakMelding: ConsumerRecord<String, Siste14aVedtakKafkaDto>) {

    }

}
