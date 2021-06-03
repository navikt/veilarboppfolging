package no.nav.veilarboppfolging.config;

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
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.veilarboppfolging.config.KafkaConfig.CONSUMER_GROUP_ID;
import static no.nav.veilarboppfolging.config.KafkaConfig.PRODUCER_CLIENT_ID;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaTestConfig {

    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3";

    private final KafkaContainer kafkaContainer;

    private final KafkaConsumerClient consumerClient;

    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    private final KafkaProducerRecordProcessor producerRecordProcessor;

    private final KafkaProducerRecordStorage<String, String> producerRecordStorage;

    public KafkaTestConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties
    ) {
        kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafkaContainer.start();

        KafkaConsumerRepository consumerRepository = new OracleConsumerRepository(jdbcTemplate.getDataSource());
        KafkaProducerRepository producerRepository = new OracleProducerRepository(jdbcTemplate.getDataSource());

        Map<String, TopicConsumer<String, String>> topicConsumers = Map.of(
                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                jsonConsumer(VeilarbArenaOppfolgingEndret.class, kafkaConsumerService::consumeEndringPaOppfolgingBruker)
        );

        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties(1000)
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withDeserializers(StringDeserializer.class, StringDeserializer.class)
                .build();

        consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
                .withProperties(properties)
                .withRepository(consumerRepository)
                .withSerializers(new StringSerializer(), new StringSerializer())
                .withStoreOnFailureConsumers(topicConsumers)
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
                .withProperties(producerProperties(kafkaContainer))
                .build();

        producerRecordProcessor = new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient);
    }

    @Bean
    public KafkaContainer kafkaContainer() {
        return kafkaContainer;
    }

    @Bean
    public KafkaProducerRecordStorage<String, String> producerRecordStorage() {
        return producerRecordStorage;
    }

    private Properties producerProperties(KafkaContainer kafkaContainer) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(PRODUCER_CLIENT_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        producerRecordProcessor.start();
    }
}
