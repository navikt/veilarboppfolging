package no.nav.fo.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.json.JsonUtils;
import org.junit.Test;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static no.nav.fo.veilarboppfolging.kafka.Consumer.ENDRING_PAA_BRUKER_KAFKA_TOPIC;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Slf4j
public class ConsumerKafkaTest extends KafkaTest {

    @Inject
    private Iserv28Service iserv28Service;

    @Test
    public void testConsume() throws InterruptedException {
        ZonedDateTime iservSiden = ZonedDateTime.now();

        ArenaBruker bruker = new ArenaBruker();
        bruker.setAktoerid("1234");
        bruker.setFormidlingsgruppekode("ISERV");
        bruker.setIserv_fra_dato(iservSiden);
        assertThat(iserv28Service.eksisterendeIservBruker(bruker)).isNull();

        kafkaTemplate.send(ENDRING_PAA_BRUKER_KAFKA_TOPIC, JsonUtils.toJson(bruker));
        dynamicTimeout(() -> iserv28Service.eksisterendeIservBruker(bruker) != null);

        IservMapper iservMapper = iserv28Service.eksisterendeIservBruker(bruker);

        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo("1234");
        assertThat(iservMapper.getIservSiden()).isEqualTo(iservSiden);
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
