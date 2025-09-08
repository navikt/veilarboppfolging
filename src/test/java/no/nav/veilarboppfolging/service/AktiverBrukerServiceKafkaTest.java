package no.nav.veilarboppfolging.service;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerManueltService;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static no.nav.veilarboppfolging.test.TestUtils.verifiserAsynkront;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
public class AktiverBrukerServiceKafkaTest extends IntegrationTest {

    @Autowired
    EmbeddedKafkaBroker kafkaContainer;

    @Autowired
    DataSource dataSource;

    @Autowired
    AktiverBrukerManueltService aktiverBrukerManueltService;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final Fnr fnr = Fnr.of("12345678901");
    private final AktorId aktorId = AktorId.of("09876543210987");
    private final Innsatsgruppe innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
    private AktiverArbeidssokerData aktiverArbeidssokerData;

    private KafkaConsumerClient consumerClient;
    private AtomicReference<Map<AktorId, SisteOppfolgingsperiodeV1>> konsumerteSisteOppfolgingsperiodeMeldinger = new AtomicReference<>(new HashMap<>());

    @BeforeEach
    public void setup() {
        jdbcTemplate.update("DELETE FROM KAFKA_PRODUCER_RECORD");
        konsumerteSisteOppfolgingsperiodeMeldinger.set(new HashMap<>());
        aktiverArbeidssokerData = new AktiverArbeidssokerData();
        aktiverArbeidssokerData.setFnr(new AktiverArbeidssokerData.Fnr(fnr.get()));
        aktiverArbeidssokerData.setInnsatsgruppe(innsatsgruppe);

        when(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId);
        when(aktorOppslagClient.hentFnr(aktorId)).thenReturn(fnr);

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(kafkaTestConsumerProperties(kafkaContainer.getBrokersAsString()))
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

        startOppfolgingSomArbeidsoker(aktorId, fnr);
        verifiserAsynkront(8, TimeUnit.SECONDS, () -> {
            assertEquals(1,
                    konsumerteSisteOppfolgingsperiodeMeldinger.get().values().size(),
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
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return props;
    }

    private int kafkaProducerRecordCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KAFKA_PRODUCER_RECORD", Integer.class);
    }
}
