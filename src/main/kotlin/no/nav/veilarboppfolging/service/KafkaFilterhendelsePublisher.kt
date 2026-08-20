package no.nav.veilarboppfolging.service

import jakarta.annotation.PreDestroy
import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

interface FilterhendelsePublisher {
    fun publiser(topic: String, key: String, filterhendelseRecord: FilterhendelseRecord): RecordMetadata
}

@Profile("!test")
@Component
class KafkaFilterhendelsePublisher : FilterhendelsePublisher {
    private val producerClient: KafkaProducerClient<String, String> =
        KafkaProducerClientBuilder.builder<String, String>()
            .withProperties(
                KafkaPropertiesPreset.aivenDefaultProducerProperties("veilarboppfolging-filterhendelse-producer")
            )
            .build()

    override fun publiser(topic: String, key: String, filterhendelseRecord: FilterhendelseRecord): RecordMetadata {
        val jsonPayload = JsonUtils.getMapper().writeValueAsString(filterhendelseRecord)
        return producerClient.sendSync(ProducerRecord(topic, key, jsonPayload))
    }

    @PreDestroy
    fun close() {
        producerClient.close()
    }
}
