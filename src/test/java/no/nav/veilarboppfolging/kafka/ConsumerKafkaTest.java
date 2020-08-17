package no.nav.veilarboppfolging.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.service.IservService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@Slf4j
public class ConsumerKafkaTest extends KafkaTest {

    @Autowired
    private IservService iservService;

    @Test(timeout=1000)
    @Ignore
    public void testConsume() {
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
                verify(iservService).behandleEndretBruker(eq(EndringPaOppfolgingBrukerConsumer.deserialisereBruker(kafkaMelding)));
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
