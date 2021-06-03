package no.nav.veilarboppfolging.kafka;


import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.veilarboppfolging.config.ApplicationTestConfig;
import no.nav.veilarboppfolging.config.KafkaProperties;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingStartetKafkaDTO;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.test.TestUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static no.nav.common.kafka.consumer.util.deserializer.Deserializers.jsonDeserializer;
import static no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@SpringBootTest(classes = {ApplicationTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KafkaIntegrationTest {

    @Autowired
    KafkaContainer kafkaContainer;

    @Autowired
    KafkaProperties kafkaProperties;

    @Test
    public void konsumerer_melding_om_ny_bruker_fra_arena_og_starter_oppfolging_og_produserer_melding_om_oppfolging_startet() {

        VeilarbArenaOppfolgingEndret oppfolgingEndret = new VeilarbArenaOppfolgingEndret()
                .setAktoerid(randomNumeric(10))
                .setFormidlingsgruppekode("ARBS");

        KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));

        producer.send(new ProducerRecord(
                kafkaProperties.getEndringPaaOppfolgingBrukerTopic(),
                oppfolgingEndret.getAktoerid(),
                JsonUtils.toJson(oppfolgingEndret)));

        AtomicReference<OppfolgingStartetKafkaDTO> konsumertMelding = new AtomicReference<>(null);

        KafkaConsumerClientBuilder.builder()
                .withProperties(kafkaTestConsumerProperties(kafkaContainer.getBootstrapServers()))
                .withTopicConfig(
                        new KafkaConsumerClientBuilder.TopicConfig<String, OppfolgingStartetKafkaDTO>()
                                .withConsumerConfig(
                                        kafkaProperties.getOppfolgingStartetTopic(),
                                        stringDeserializer(),
                                        jsonDeserializer(OppfolgingStartetKafkaDTO.class),
                                        record -> {
                                            konsumertMelding.set(record.value());
                                        }
                                )
                )
                .build()
                .start();

        TestUtils.verifiserAsynkront(100, TimeUnit.SECONDS, () -> {
            assertEquals(oppfolgingEndret.getAktoerid(), konsumertMelding.get().getAktorId());
        });
    }

    public static Properties kafkaTestConsumerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5 * 60 * 1000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
