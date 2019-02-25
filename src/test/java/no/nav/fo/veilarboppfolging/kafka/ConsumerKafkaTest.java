package no.nav.fo.veilarboppfolging.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.json.JsonUtils;

import org.junit.Test;
import org.springframework.kafka.support.SendResult;

import javax.inject.Inject;

import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsumerKafkaTest extends KafkaTest {

    @Inject
    private Iserv28Service iserv28Service;

    @Test
    public void testConsume() throws InterruptedException {
        ArenaBruker bruker = new ArenaBruker();
        bruker.setAktoerid("1234");
        bruker.setFodselsnr("4321");
        bruker.setFormidlingsgruppekode("ISERV");
        bruker.setIserv_fra_dato(ZonedDateTime.now());
        bruker.setKvalifiseringsgruppekode("KVALKODE");

        String kafkaMelding = JsonUtils.toJson(bruker);
        
        send(KafkaTestConfig.KAFKA_TEST_TOPIC, kafkaMelding);
        Thread.sleep(50);
        verify(iserv28Service).behandleEndretBruker(eq(Consumer.deserialisereBruker(kafkaMelding)));

    }

    @SneakyThrows
    private void send(String topic, String message) {
        SendResult<String, String> sendResult = kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
        log.info("{}", sendResult);
    }

}
