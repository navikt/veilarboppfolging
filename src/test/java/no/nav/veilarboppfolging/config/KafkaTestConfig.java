package no.nav.veilarboppfolging.config;

import net.javacrumbs.shedlock.core.LockProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.veilarboppfolging.config.KafkaConfig.CONSUMER_GROUP_ID;
import static no.nav.veilarboppfolging.config.KafkaConfig.PRODUCER_CLIENT_ID;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaTestConfig {

    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3";

    @Bean
    public KafkaContainer kafkaContainer() {
        KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Autowired
    KafkaConsumerClient<String, String> consumerClient;

    @Autowired
    KafkaConsumerRecordProcessor consumerRecordProcessor;

    @Autowired
    KafkaProducerRecordProcessor producerRecordProcessor;


    @Bean
    public KafkaConsumerRepository kafkaConsumerRepository(DataSource dataSource) {
        return new OracleConsumerRepository(dataSource);
    }

    @Bean
    public KafkaProducerRepository producerRepository(DataSource dataSource) {
        return new OracleProducerRepository(dataSource);
    }

    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            KafkaConsumerService endringPaOppfolgingBrukerConsumerService,
            KafkaProperties kafkaProperties
    ) {
        return Map.of(
                kafkaProperties.getEndringPaaOppfolgingBruker(),
                jsonConsumer(VeilarbArenaOppfolgingEndret.class, endringPaOppfolgingBrukerConsumerService::consumeEndringPaOppfolgingBruker)
        );
    }

    @Bean
    public KafkaConsumerClient<String, String> consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            KafkaConsumerRepository kafkaConsumerRepository,
            KafkaContainer kafkaContainer
    ) {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties(1000)
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withDeserializers(StringDeserializer.class, StringDeserializer.class)
                .build();

        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(properties)
                .withRepository(kafkaConsumerRepository)
                .withSerializers(new StringSerializer(), new StringSerializer())
                .withStoreOnFailureConsumers(topicConsumers)
                .withLogging()
                .build();
    }

    @Bean
    public KafkaConsumerRecordProcessor consumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaConsumerRepository,
            Map<String, TopicConsumer<String, String>> topicConsumers
    ) {
        Map<String, StoredRecordConsumer> storedRecordConsumers = ConsumerUtils.toStoredRecordConsumerMap(
                topicConsumers,
                new StringDeserializer(),
                new StringDeserializer()
        );

        return KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(kafkaConsumerRepository)
                .withRecordConsumers(storedRecordConsumers)
                .build();
    }

    @Bean
    public KafkaProducerRecordStorage<String, String> producerRecordStorage(KafkaProducerRepository kafkaProducerRepository) {
        return new KafkaProducerRecordStorage<>(
                kafkaProducerRepository,
                new StringSerializer(),
                new StringSerializer()
        );
    }

    @Bean
    public KafkaProducerRecordProcessor producerRecordProcessor(
            LeaderElectionClient leaderElectionClient,
            KafkaProducerRepository producerRepository,
            KafkaContainer kafkaContainer
    ) {
        KafkaProducerClient<byte[], byte[]> producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(producerProperties(kafkaContainer))
                .build();

        return new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient);
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
