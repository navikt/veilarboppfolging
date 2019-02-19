package no.nav.fo.veilarboppfolging.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.json.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.support.SendResult;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
public class ConsumerKafkaTest extends KafkaTest {

    @Inject
    private Iserv28Service iserv28Service;

    @Inject
    private OppfolgingsStatusRepository oppfolgingStatusRepository;

    @Before
    public void setup() throws Exception {
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(true));
    }

    @Test
    public void testConsume() throws InterruptedException {
        ZonedDateTime iservSiden = ZonedDateTime.now();

        ArenaBruker bruker = new ArenaBruker();
        bruker.setAktoerid("1234");
        bruker.setFodselsnr("4321");
        bruker.setFormidlingsgruppekode("ISERV");
        bruker.setIserv_fra_dato(iservSiden);
        assertThat(iserv28Service.eksisterendeIservBruker(bruker)).isNull();

        send(KafkaTestConfig.KAFKA_TEST_TOPIC, JsonUtils.toJson(bruker));
        dynamicTimeout(() -> iserv28Service.eksisterendeIservBruker(bruker) != null);
        IservMapper iservMapper = iserv28Service.eksisterendeIservBruker(bruker);

        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo("1234");
        assertThat(iservMapper.getIservSiden()).isEqualTo(iservSiden);
    }

    @SneakyThrows
    private void send(String topic, String message) {
        SendResult<String, String> sendResult = kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
        log.info("{}", sendResult);
    }

    private void dynamicTimeout(Supplier<Boolean> waitUntil) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!waitUntil.get()) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - start > 30000) {
                throw new IllegalStateException();
            }
        }
        log.info("timeout was {}ms", System.currentTimeMillis() - start);
    }

}
