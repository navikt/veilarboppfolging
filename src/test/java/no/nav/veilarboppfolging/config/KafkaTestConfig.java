package no.nav.veilarboppfolging.config;

import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.OracleConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.OracleProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
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

    private final KafkaProducerRecordStorage producerRecordStorage;

    public KafkaTestConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            LockProvider lockProvider,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties
    ) {
        kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafkaContainer.start();

        KafkaConsumerRepository consumerRepository = new OracleConsumerRepository(jdbcTemplate.getDataSource());
        KafkaProducerRepository producerRepository = new OracleProducerRepository(jdbcTemplate.getDataSource());

        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs = List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, EndringPaaOppfoelgingsBrukerV1>()
                        .withLogging()
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(EndringPaaOppfoelgingsBrukerV1.class),
                                kafkaConsumerService::consumeEndringPaOppfolgingBruker
                        )
        );

        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties(1000)
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(properties)
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();

        producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

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
    public KafkaProducerRecordStorage producerRecordStorage() {
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
