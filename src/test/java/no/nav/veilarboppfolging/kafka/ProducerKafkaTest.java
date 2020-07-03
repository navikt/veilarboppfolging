package no.nav.veilarboppfolging.kafka;

import no.nav.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.veilarboppfolging.repository.AvsluttOppfolgingEndringRepository;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static no.nav.common.json.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;

public class ProducerKafkaTest extends KafkaTest {

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;
    private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    @Before
    public void setup() {
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(LocalH2Database.getDb());
        avsluttOppfolgingProducer = new AvsluttOppfolgingProducer(new KafkaTopics(), kafkaTemplate, avsluttOppfolgingEndringRepository);
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
