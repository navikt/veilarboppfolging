package no.nav.veilarboppfolging.config

import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.spring.PostgresJdbcTemplateProducerRepository
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import java.util.List

@Profile("!test")
@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
open class KafkaProducerConfig(
    meterRegistry: MeterRegistry,
    leaderElectionClient: LeaderElectionClient,
    kafkaProperties: KafkaProperties,
    jdbcTemplate: JdbcTemplate,
    @Value("\${app.kafka.enabled}") val kafkaEnabled: Boolean
) {

    private val producerRepository = PostgresJdbcTemplateProducerRepository(jdbcTemplate);
    val PRODUCER_CLIENT_ID: String = "veilarboppfolging-producer"

    private val aivenProducerRecordProcessor: KafkaProducerRecordProcessor

    @Bean
    open fun producerRecordProcessor(): KafkaProducerRecordStorage {
        return KafkaProducerRecordStorage(producerRepository)
    }

    init {
        val aivenProducerClient = KafkaProducerClientBuilder.builder<ByteArray, ByteArray>()
            .withProperties(KafkaPropertiesPreset.aivenByteProducerProperties(PRODUCER_CLIENT_ID))
            .withMetrics(meterRegistry)
            .build()

        aivenProducerRecordProcessor = KafkaProducerRecordProcessor(
            producerRepository,
            aivenProducerClient,
            leaderElectionClient,
            List.of(
                kafkaProperties.getSisteOppfolgingsperiodeTopic(),
                kafkaProperties.getSisteTilordnetVeilederTopic(),
                kafkaProperties.getVeilederTilordnetTopic(),
                kafkaProperties.getOppfolgingsperiodeTopic(),
                kafkaProperties.getEndringPaManuellStatusTopic(),
                kafkaProperties.getEndringPaNyForVeilederTopic(),
                kafkaProperties.getEndringPaMalAiven(),
                kafkaProperties.getKvpAvsluttetTopicAiven(),
                kafkaProperties.getKvpStartetTopicAiven(),
                kafkaProperties.getKvpPerioderTopicAiven(),
                kafkaProperties.getMinSideAapenMicrofrontendV1()
            )
        )

        if (kafkaEnabled) {
            aivenProducerRecordProcessor.start()
        }
    }
}