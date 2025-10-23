package no.nav.veilarboppfolging.config

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.LockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.utils.EnvironmentUtils
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.kafka.ArbeidsoppfolgingskontortilordningConsumerService
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import java.util.Map
import java.util.function.Consumer

@Profile("!test")
@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
open class KafkaConsumerConfig(
    jdbcTemplate: JdbcTemplate,
    meterRegistry: MeterRegistry,
    kafkaProperties: KafkaProperties,
    private val kafkaConsumerService: KafkaConsumerService,
    private val arbeidssøkerperiodeConsumerService: ArbeidssøkerperiodeConsumerService,
    private val arbeidsoppfolgingskontortilordningConsumerService: ArbeidsoppfolgingskontortilordningConsumerService,
    lockProvider: LockProvider,
    @Value("\${app.kafka.enabled}") val kafkaEnabled: Boolean
) {

    val CONSUMER_GROUP_ID: String = "veilarboppfolging-consumer"

    private var aivenConsumerClient: KafkaConsumerClient? = null
    private var consumerRecordProcessor: KafkaConsumerRecordProcessor? = null

    init {
        val consumerRepository: KafkaConsumerRepository = PostgresJdbcTemplateConsumerRepository(jdbcTemplate)
        if (kafkaEnabled) {
            val topicConfigs = listOf<KafkaConsumerClientBuilder.TopicConfig<*, *>>(
                KafkaConsumerClientBuilder.TopicConfig<String, EndringPaaOppfoelgingsBrukerV2>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            EndringPaaOppfoelgingsBrukerV2::class.java
                        ),
                        Consumer<ConsumerRecord<String, EndringPaaOppfoelgingsBrukerV2>> { kafkaMelding: ConsumerRecord<String, EndringPaaOppfoelgingsBrukerV2>? ->
                            kafkaConsumerService.consumeEndringPaOppfolgingBruker(
                                kafkaMelding
                            )
                        }
                    ),
                KafkaConsumerClientBuilder.TopicConfig<String, Periode>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.getArbeidssokerperioderTopicAiven(),
                        Deserializers.stringDeserializer(),
                        getPeriodeAvroDeserializer(),
                        Consumer<ConsumerRecord<String, Periode>> { kafkaMelding: ConsumerRecord<String, Periode> ->
                            arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(
                                kafkaMelding
                            )
                        }
                    ),
//                KafkaConsumerClientBuilder.TopicConfig<String, OppfolgingskontorMelding?>()
//                    .withLogging()
//                    .withMetrics(meterRegistry)
//                    .withStoreOnFailure(consumerRepository)
//                    .withConsumerConfig(
//                        kafkaProperties.arbeidsoppfolgingskontortilordningTopic,
//                        Deserializers.stringDeserializer(),
//                        Deserializers.jsonDeserializer(
//                            OppfolgingskontorMelding::class.java
//                        ),
//                        Consumer { kafkaMelding: ConsumerRecord<String, OppfolgingskontorMelding?> ->
//                            arbeidsoppfolgingskontortilordningConsumerService.consumeKontortilordning(kafkaMelding)
//                        }
//                    )
            )

            val aivenConsumerProperties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withDeserializers(
                    ByteArrayDeserializer::class.java,
                    ByteArrayDeserializer::class.java
                )
                .build()

            aivenConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties)
                .withTopicConfigs(topicConfigs)
                .build()

            consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(ConsumerUtils.findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build()

            aivenConsumerClient?.start()
            consumerRecordProcessor?.start()
        }
    }

    private fun getPeriodeAvroDeserializer(): Deserializer<Periode> {
        val schemaUrl = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY")
        val avroDeserializer = Deserializers.aivenAvroDeserializer<Periode>()
        avroDeserializer.configure(
            Map.of(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true
            ),
            false
        )
        return avroDeserializer
    }
}