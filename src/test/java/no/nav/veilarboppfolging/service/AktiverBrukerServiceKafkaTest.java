package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.pto_schema.kafka.json.topic.onprem.OppfolgingStartetV1;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.config.ApplicationTestConfig;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static no.nav.veilarboppfolging.test.TestUtils.verifiserAsynkront;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AktiverBrukerServiceKafkaTest {

    @MockBean
    AktorregisterClient aktorregisterClient;

    @Autowired
    KafkaContainer kafkaContainer;

    @Autowired
    DataSource dataSource;

    @Autowired
    AktiverBrukerService aktiverBrukerService;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    BehandleArbeidssokerClient behandleArbeidssokerClient;

    private final Fnr fnr = Fnr.of("123");
    private final AktorId aktorId = AktorId.of("987654321");
    private final Innsatsgruppe innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
    private AktiverArbeidssokerData aktiverArbeidssokerData;

    private KafkaConsumerClient consumerClient;
    private AtomicReference<Map<AktorId, OppfolgingStartetV1>> konsumerteOppfolgingStartetMeldinger = new AtomicReference<>(new HashMap<>());
    private AtomicReference<Map<AktorId, SisteOppfolgingsperiodeV1>> konsumerteSisteOppfolgingsperiodeMeldinger = new AtomicReference<>(new HashMap<>());

    @BeforeEach
    public void setup() {
        konsumerteOppfolgingStartetMeldinger.set(new HashMap<>());
        konsumerteSisteOppfolgingsperiodeMeldinger.set(new HashMap<>());
        aktiverArbeidssokerData = new AktiverArbeidssokerData();
        aktiverArbeidssokerData.setFnr(new AktiverArbeidssokerData.Fnr(fnr.get()));
        aktiverArbeidssokerData.setInnsatsgruppe(innsatsgruppe);

        when(aktorregisterClient.hentAktorId(fnr)).thenReturn(aktorId);
        when(aktorregisterClient.hentFnr(aktorId)).thenReturn(fnr);

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(kafkaTestConsumerProperties(kafkaContainer.getBootstrapServers()))
                .withTopicConfig(
                        new KafkaConsumerClientBuilder.TopicConfig<String, OppfolgingStartetV1>()
                                .withConsumerConfig(
                                        "oppfolgingStartet-topic",
                                        Deserializers.stringDeserializer(),
                                        Deserializers.jsonDeserializer(OppfolgingStartetV1.class),
                                        record -> {
                                            konsumerteOppfolgingStartetMeldinger.get().put(AktorId.of(record.key()), record.value());
                                            return ConsumeStatus.OK;
                                        }
                                ))
                .withTopicConfig(
                        new KafkaConsumerClientBuilder.TopicConfig<String, SisteOppfolgingsperiodeV1>()
                                .withConsumerConfig(
                                        "sisteOppfolgingsperiode-topic",
                                        Deserializers.stringDeserializer(),
                                        Deserializers.jsonDeserializer(SisteOppfolgingsperiodeV1.class),
                                        record -> {
                                            konsumerteSisteOppfolgingsperiodeMeldinger.get().put(AktorId.of(record.key()), record.value());
                                            return ConsumeStatus.OK;
                                        }
                                )
                )
                .build();
    }

    @Test
    public void skalPubliserePaaKafkaVedAktivering() {

        consumerClient.start();

        aktiverBrukerService.aktiverBruker(Fnr.of(aktiverArbeidssokerData.getFnr().getFnr()), aktiverArbeidssokerData.getInnsatsgruppe());

        verifiserAsynkront(8, TimeUnit.SECONDS, () -> {
            assertEquals(1,
                    konsumerteOppfolgingStartetMeldinger.get().values().size(),
                    "Skal ikke ha konsumert kafka-melding for oppfølging startet fra transaksjon som rulles tilbake");
            assertEquals(1,
                    konsumerteSisteOppfolgingsperiodeMeldinger.get().values().size(),
                    "Skal ikke ha konsumert kafka-melding for siste oppfølgingsperiode fra transaksjon som rulles tilbake");
        });

        consumerClient.stop();
    }

    @Test
    @SneakyThrows
    public void skalIkkePubliserePaaKafkaDersomTransaksjonRullesTilbakeVedAktivering() {

        doAnswer(invocationOnMock -> {
            throw new RuntimeException("Feiler fra Arena");
        }).when(behandleArbeidssokerClient).opprettBrukerIArena(fnr, innsatsgruppe);

        consumerClient.start();

        boolean feilet = false;
        try {
            aktiverBrukerService.aktiverBruker(Fnr.of(aktiverArbeidssokerData.getFnr().getFnr()), aktiverArbeidssokerData.getInnsatsgruppe());
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("Feiler fra Arena")) {
                throw e;
            } else {
                feilet = true;
            }
        }
        Thread.sleep(1000);
        assertTrue(feilet, "Skulle ha feilet");

        verifiserAsynkront(8, TimeUnit.SECONDS, () -> {
            assertEquals(0, kafkaProducerRecordCount());
            assertTrue(
                    konsumerteOppfolgingStartetMeldinger.get().values().isEmpty(),
                    "Skal ikke ha konsumert kafka-melding for oppfølging startet fra transaksjon som rulles tilbake");
            assertTrue(
                    konsumerteSisteOppfolgingsperiodeMeldinger.get().values().isEmpty(),
                    "Skal ikke ha konsumert kafka-melding for siste oppfølgingsperiode fra transaksjon som rulles tilbake");
        });

        consumerClient.stop();
    }

    private Properties kafkaTestConsumerProperties(String brokerUrl) {
        var props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5 * 60 * 1000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private int kafkaProducerRecordCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KAFKA_PRODUCER_RECORD", Integer.class);
    }
}
