package no.nav.veilarboppfolging.config;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.spring.OracleJdbcTemplateConsumerRepository;
import no.nav.common.kafka.spring.OracleJdbcTemplateProducerRepository;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenByteProducerProperties;


@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarboppfolging-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarboppfolging-producer";

    private final KafkaConsumerClient aivenConsumerClient;

    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    private final KafkaProducerRecordProcessor aivenProducerRecordProcessor;

    private final KafkaProducerRecordStorage producerRecordStorage;


    public KafkaConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            LockProvider lockProvider,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        KafkaConsumerRepository consumerRepository = new OracleJdbcTemplateConsumerRepository(jdbcTemplate);
        KafkaProducerRepository producerRepository = new OracleJdbcTemplateProducerRepository(jdbcTemplate);

        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs = List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, EndringPaaOppfoelgingsBrukerV2>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(EndringPaaOppfoelgingsBrukerV2.class),
                                kafkaConsumerService::consumeEndringPaOppfolgingBruker
                        )
        );

        Properties aivenConsumerProperties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        aivenConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties)
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();

        producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

        KafkaProducerClient<byte[], byte[]> aivenProducerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(aivenByteProducerProperties(PRODUCER_CLIENT_ID))
                .withMetrics(meterRegistry)
                .build();

        aivenProducerRecordProcessor = new KafkaProducerRecordProcessor(
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
                        kafkaProperties.getKvpStartetTopicAiven()
                )
        );
    }

    @Bean
    public KafkaProducerRecordStorage producerRecordProcessor() {
        return producerRecordStorage;
    }

    @PostConstruct
    public void start() {
        aivenConsumerClient.start();
        consumerRecordProcessor.start();
        aivenProducerRecordProcessor.start();
    }
}
