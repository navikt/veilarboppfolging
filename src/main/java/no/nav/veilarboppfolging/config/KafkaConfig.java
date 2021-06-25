package no.nav.veilarboppfolging.config;

import io.micrometer.core.instrument.MeterRegistry;
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
import no.nav.common.utils.Credentials;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.*;


@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarboppfolging-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarboppfolging-producer";

    private final KafkaConsumerClient consumerClient;

    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    private final KafkaProducerRecordProcessor onPremProducerRecordProcessor;

    private final KafkaProducerRecordProcessor aivenProducerRecordProcessor;

    private final KafkaProducerRecordStorage producerRecordStorage;


    public KafkaConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            LockProvider lockProvider,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            Credentials credentials,
            MeterRegistry meterRegistry
    ) {
        KafkaConsumerRepository consumerRepository = new OracleConsumerRepository(jdbcTemplate.getDataSource());
        KafkaProducerRepository producerRepository = new OracleProducerRepository(jdbcTemplate.getDataSource());

        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs = List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, EndringPaaOppfoelgingsBrukerV1>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(EndringPaaOppfoelgingsBrukerV1.class),
                                kafkaConsumerService::consumeEndringPaOppfolgingBruker
                        )
        );

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();

        producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

        KafkaProducerClient<byte[], byte[]> onPremProducerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(onPremByteProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withMetrics(meterRegistry)
                .build();

        onPremProducerRecordProcessor = new KafkaProducerRecordProcessor(
                producerRepository,
                onPremProducerClient,
                leaderElectionClient,
                List.of(
                        kafkaProperties.getEndringPaManuellStatusTopic(),
                        kafkaProperties.getEndringPaNyForVeilederTopic(),
                        kafkaProperties.getVeilederTilordnetTopic(),
                        kafkaProperties.getOppfolgingStartetTopic(),
                        kafkaProperties.getOppfolgingAvsluttetTopic(),
                        kafkaProperties.getEndringPaaAvsluttOppfolgingTopic(),
                        kafkaProperties.getKvpStartetTopic(),
                        kafkaProperties.getKvpAvlsuttetTopic(),
                        kafkaProperties.getEndringPaMalTopic()
                )
        );

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
                        kafkaProperties.getSisteTilordnetVeilederTopic()
                )
        );
    }

    @Bean
    public KafkaProducerRecordStorage producerRecordProcessor() {
        return producerRecordStorage;
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        onPremProducerRecordProcessor.start();
        aivenProducerRecordProcessor.start();
    }
}
