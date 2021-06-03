package no.nav.veilarboppfolging.config;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.OracleConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoredRecordConsumer;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.OracleProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremByteProducerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;


@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarboppfolging-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarboppfolging-producer";

    private final KafkaConsumerClient consumerClient;

    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    private final KafkaProducerRecordProcessor producerRecordProcessor;

    private final KafkaProducerRecordStorage<String, String> producerRecordStorage;

    public KafkaConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            Credentials credentials,
            MeterRegistry meterRegistry
    ) {
        KafkaConsumerRepository consumerRepository = new OracleConsumerRepository(jdbcTemplate.getDataSource());
        KafkaProducerRepository producerRepository = new OracleProducerRepository(jdbcTemplate.getDataSource());

        Map<String, TopicConsumer<String, String>> topicConsumers = Map.of(
                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                jsonConsumer(VeilarbArenaOppfolgingEndret.class, kafkaConsumerService::consumeEndringPaOppfolgingBruker)
        );

        consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withRepository(consumerRepository)
                .withSerializers(new StringSerializer(), new StringSerializer())
                .withStoreOnFailureConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();

        Map<String, StoredRecordConsumer> storedRecordConsumers = ConsumerUtils.toStoredRecordConsumerMap(
                topicConsumers,
                new StringDeserializer(),
                new StringDeserializer()
        );

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(new JdbcTemplateLockProvider(jdbcTemplate))
                .withKafkaConsumerRepository(consumerRepository)
                .withRecordConsumers(storedRecordConsumers)
                .build();

        producerRecordStorage = new KafkaProducerRecordStorage<>(
                producerRepository,
                new StringSerializer(),
                new StringSerializer()
        );

        KafkaProducerClient<byte[], byte[]> producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(onPremByteProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withMetrics(meterRegistry)
                .build();

        producerRecordProcessor = new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient);
    }

    @Bean
    public KafkaProducerRecordStorage<String, String> producerRecordProcessor() {
        return producerRecordStorage;
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        producerRecordProcessor.start();
    }
}
