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
import no.nav.common.utils.Credentials;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.service.KafkaConsumerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.*;


@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarboppfolging-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarboppfolging-producer";

    private final KafkaConsumerClient aivenConsumerClient;

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

        /*
         * Setter AUTO_OFFSET_RESET_CONFIG (auto.offset.reset) til "none". Dette fordi konsument for topic
         * pto.endring-paa-oppfolgingsbruker-v2 ikke sjekker om en melding er lest fra før.
         *
         * F.eks. for en gitt bruker:
         *  - melding 1 = "start oppfølging"
         *  - melding 2 = "avslutt oppfølging"
         *  - melding 3 = "start oppfølging"
         * Dersom melding 2 og 3 leses på nytt, så vil oppfølgingsperiode avsluttes, og en ny startes.
         */
        Properties aivenConsumerProperties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .withProp(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none")
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

        KafkaProducerClient<byte[], byte[]> onPremProducerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(onPremByteProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withMetrics(meterRegistry)
                .build();

        onPremProducerRecordProcessor = new KafkaProducerRecordProcessor(
                producerRepository,
                onPremProducerClient,
                leaderElectionClient,
                List.of(
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
                        kafkaProperties.getSisteTilordnetVeilederTopic(),
                        kafkaProperties.getEndringPaManuellStatusTopic(),
                        kafkaProperties.getEndringPaNyForVeilederTopic()
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
        onPremProducerRecordProcessor.start();
        aivenProducerRecordProcessor.start();
    }
}
