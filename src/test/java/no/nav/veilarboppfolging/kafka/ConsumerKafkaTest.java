package no.nav.veilarboppfolging.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.utils.mappers.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.services.Iserv28Service;
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

    @Test(timeout=1000)
    public void testConsume() throws InterruptedException {
        VeilarbArenaOppfolgingEndret bruker = new VeilarbArenaOppfolgingEndret();
        bruker.setAktoerid("1234");
        bruker.setFodselsnr("4321");
        bruker.setFormidlingsgruppekode("ISERV");
        bruker.setIserv_fra_dato(ZonedDateTime.now());
        bruker.setKvalifiseringsgruppekode("KVALKODE");

        String kafkaMelding = JsonUtils.toJson(bruker);
        
        send(KafkaTestConfig.KAFKA_TEST_TOPIC, kafkaMelding);
        verifiserKonsumentAsynkront(kafkaMelding);

    }

    private void verifiserKonsumentAsynkront(String kafkaMelding) {
        boolean prosessert = false;
        while(!prosessert) {
            try {
                Thread.sleep(10);
                verify(iserv28Service).behandleEndretBruker(eq(Consumer.deserialisereBruker(kafkaMelding)));
                prosessert = true;
            } catch(Throwable a) {
            }
        }
    }

    @SneakyThrows
    private void send(String topic, String message) {
        SendResult<String, String> sendResult = kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
        log.info("{}", sendResult);
    }

}
