package no.nav.fo.veilarboppfolging.kafka;
import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.json.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;

public class ProducerKafkaTest extends KafkaTest {

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;
    private JdbcTemplate db;
    private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    @Before
    public void setup() {
        db = new JdbcTemplate(setupInMemoryDatabase());
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(db);
        avsluttOppfolgingProducer = new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, RECEIVER_TOPIC);
    }

    @Test
    public void kafka_send_avslutt_oppfolging() throws InterruptedException {
        String aktorId = "1234";
        LocalDateTime avsluttOppfolgingDato = LocalDateTime.now();
        String serialisertBruker = serialiserBruker(aktorId, avsluttOppfolgingDato);
        avsluttOppfolgingProducer.avsluttOppfolgingEvent(aktorId, avsluttOppfolgingDato);
        ConsumerRecord<String, String> received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received.value()).isEqualTo(serialisertBruker);
    }

    private String serialiserBruker (String aktorId, LocalDateTime avsluttOppfolgingDato) {
        AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO = AvsluttOppfolgingProducer.toDTO(aktorId, avsluttOppfolgingDato);
        return toJson(avsluttOppfolgingKafkaDTO);
    }
}
