package no.nav.veilarboppfolging.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.utils.EnvironmentUtils
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.config.KafkaTestConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*


@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssøkerperiodeKafkaTest: IntegrationTest() {

    @Autowired
    private lateinit var kafkaContainer: EmbeddedKafkaBroker

    private lateinit var producer: KafkaProducerClientImpl<String, Periode>

    private val topic = "arbeidssokerperioder-v1-topic"

    @BeforeAll
    fun beforeAll() {
        producer = KafkaProducerClientImpl<String, Periode>(kafkaTestProducerProperties())
    }

    @Test
    fun skalKunneKonsumereMelding() {
        producer.send(ProducerRecord(topic, "dummyAktørId", periode()))
    }

    private fun periode() = Periode().apply {
        id = UUID.randomUUID()
        identitetsnummer = "dummy"
        startet = MetaData().apply {
            tidspunkt = Instant.now()
            aarsak = "dummy"
            utfoertAv = Bruker(
                BrukerType.VEILEDER,
                "dummyId"
            )
            kilde = "dummy"
            tidspunktFraKilde = TidspunktFraKilde(
                Instant.now(),
                AvviksType.FORSINKELSE
            )
        }
    }

    private fun kafkaTestProducerProperties() = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.brokersAsString)
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString())
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://testUrl")
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000)
            put(ProducerConfig.LINGER_MS_CONFIG, 100)
        }

//    private fun getSerializer(): KafkaAvroSerializer {
//        val mockSchemaRegistryClient = MockSchemaRegistryClient()
//        val map: MutableMap<String, Any> = HashMap()
//        map[KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS] = true
//        map[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "localhost:8888"
//        val serializer = KafkaAvroSerializer(mockSchemaRegistryClient)
//        serializer.configure(map, false)
//        return serializer
//    }

//    class PeriodeKafkaAvroSerializer : KafkaAvroSerializer(
//        MockSchemaRegistry.getClientForScope("arbeidssøkerperiode-scope").apply {
//            register("subject", AvroSchema(Schema.SCHEMA_EXAMPLE))
//        }
//    ) {
//        init {
//
//            super.schemaRegistry = MockSchemaRegistryClient()
//            val map: MutableMap<String, Any> = HashMap()
//
//            map["schema.registry.url"] = "localhost:8888"
//            map.put("KAFKA_SCHEMA_REGISTRY", "http://localhost:8081");
//            map.put("KAFKA_SCHEMA_REGISTRY", "http://localhost:8081");
//            map[KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS] = true
//            map[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "localhost:8888"
//            super.configure(KafkaAvroSerializerConfig(map))
//            super.configure(map, false)
//            super.serializerConfig(map)
//            super.serializ
//        }
//    }

//    class PeriodeKafkaAvroSerializer : KafkaAvroSerializer(MockSchemaRegistryClient()) {
//        init {
//            super.schemaRegistry = MockSchemaRegistryClient()
//            val map: MutableMap<String, Any> = HashMap()
//
//            map["schema.registry.url"] = "localhost:8888"
//            map.put("KAFKA_SCHEMA_REGISTRY", "http://localhost:8081");
//            map.put("KAFKA_SCHEMA_REGISTRY", "http://localhost:8081");
//            map[KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS] = true
//            map[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "localhost:8888"
//            super.configure(KafkaAvroSerializerConfig(map))
//            super.configure(map, false)
//            super.serializerConfig(map)
//            super.serializ
//        }
//    }
}