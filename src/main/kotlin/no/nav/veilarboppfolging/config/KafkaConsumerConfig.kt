package no.nav.veilarboppfolging.config

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.micrometer.core.instrument.MeterRegistry
import java.util.Map
import java.util.function.Consumer
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
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kafka.AapHendelserConsumerService
import no.nav.veilarboppfolging.kafka.ArbeidsoppfolgingskontortilordningConsumerService
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.PersonhendelseConsumer
import no.nav.veilarboppfolging.kafka.inngang.StartOppfolgingConsumerService
import no.nav.veilarboppfolging.kafka.inngang.StartOppfolgingMelding
import no.nav.veilarboppfolging.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate

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
    private val aapHendelserConsumerService: AapHendelserConsumerService,
    private val startOppfolgingConsumerService: StartOppfolgingConsumerService,
    private val personhendelseConsumer: PersonhendelseConsumer,
    lockProvider: LockProvider,
    @Value("\${app.kafka.enabled}") val kafkaEnabled: Boolean
) {

    val CONSUMER_GROUP_ID: String = "veilarboppfolging-consumer"
    val PERSONHENDELSE_CONSUMER_GROUP_ID: String = "veilarboppfolging-personhendelse-consumer"

    private var aivenConsumerClient: KafkaConsumerClient? = null
    private var personhendelseConsumerClient: KafkaConsumerClient? = null
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
                        kafkaProperties.endringPaaOppfolgingBrukerTopic,
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
                        kafkaProperties.arbeidssokerperioderTopicAiven,
                        Deserializers.stringDeserializer(),
                        avroDeserializer<Periode>(),
                        Consumer<ConsumerRecord<String, Periode>> { kafkaMelding: ConsumerRecord<String, Periode> ->
                            arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(
                                kafkaMelding
                            )
                        }
                    ),
                KafkaConsumerClientBuilder.TopicConfig<Long, OppfolgingskontorMelding?>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.arbeidsoppfolgingskontortilordningTopic,
                        Deserializers.longDeserializer(),
                        Deserializers.jsonDeserializer(
                            OppfolgingskontorMelding::class.java
                        ),
                        Consumer { kafkaMelding: ConsumerRecord<Long, OppfolgingskontorMelding?> ->
                            arbeidsoppfolgingskontortilordningConsumerService.consumeKontortilordning(kafkaMelding)
                        }
                    ),
                KafkaConsumerClientBuilder.TopicConfig<String, AapHendelseRecord>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.aapHendelserTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            AapHendelseRecord::class.java
                        ),
                        Consumer { kafkaMelding: ConsumerRecord<String, AapHendelseRecord> ->
                            aapHendelserConsumerService.consumeAapHendelse(kafkaMelding)
                        }
                    ),
                KafkaConsumerClientBuilder.TopicConfig<String, StartOppfolgingMelding>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.startOppfolgingTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            StartOppfolgingMelding::class.java
                        ),
                        Consumer { kafkaMelding: ConsumerRecord<String, StartOppfolgingMelding> ->
                            startOppfolgingConsumerService.consumeStartOppfolging(kafkaMelding)
                        }
                    ),
            )

            val personhendelseTopicConfig = KafkaConsumerClientBuilder.TopicConfig<String, Personhendelse?>()
                .withLogging()
                .withMetrics(meterRegistry)
                .withStoreOnFailure(consumerRepository)
                .withConsumerConfig(
                    kafkaProperties.pdlLeesahTopic,
                    Deserializers.stringDeserializer(),
                    avroDeserializer<Personhendelse>(),
                    Consumer<ConsumerRecord<String, Personhendelse?>> { kafkaMelding: ConsumerRecord<String, Personhendelse?> ->
                        personhendelseConsumer.consumePersonhendelse(
                            kafkaMelding
                        )
                    }
                )

            aivenConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties(CONSUMER_GROUP_ID))
                .withTopicConfigs(topicConfigs)
                .build()

            personhendelseConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties(PERSONHENDELSE_CONSUMER_GROUP_ID, autoOffsetReset = "latest"))
                .withTopicConfig(personhendelseTopicConfig)
                .build()

            consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withMetrics(meterRegistry)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(ConsumerUtils.findConsumerConfigsWithStoreOnFailure(topicConfigs + personhendelseTopicConfig))
                .build()

            aivenConsumerClient?.start()
            personhendelseConsumerClient?.start()
            consumerRecordProcessor?.start()
        }
    }

    private fun aivenConsumerProperties(consumerGroupId: String, autoOffsetReset: String = "earliest") = KafkaPropertiesBuilder.consumerBuilder()
        .withBaseProperties()
        .withConsumerGroupId(consumerGroupId)
        .withAivenBrokerUrl()
        .withAivenAuth()
        .withProp(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset)
        .withDeserializers(
            ByteArrayDeserializer::class.java,
            ByteArrayDeserializer::class.java
        )
        .build()

    private inline fun <reified T : Any> avroDeserializer(): Deserializer<T?> {
        leggTilSerializableAvroPackage(T::class.java.packageName)
        val schemaUrl = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY")
        val avroDeserializer = Deserializers.aivenAvroDeserializer<T?>()
        avroDeserializer.configure(
            Map.of(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true
            ),
            false
        )
        return avroDeserializer
    }

    private fun leggTilSerializableAvroPackage(pakke: String) {
        val eksisterende = System.getProperty(AVRO_SERIALIZABLE_PACKAGES)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            .orEmpty()
        System.setProperty(AVRO_SERIALIZABLE_PACKAGES, (eksisterende + pakke).distinct().joinToString(","))
    }

    private companion object {
        const val AVRO_SERIALIZABLE_PACKAGES = "org.apache.avro.SERIALIZABLE_PACKAGES"
    }
}