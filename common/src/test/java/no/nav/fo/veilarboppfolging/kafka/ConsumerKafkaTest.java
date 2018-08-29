package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConsumerConfig.class)
public class ConsumerKafkaTest extends KafkaTest {

    @Autowired
    private Consumer consumer;

    @Test
    public void testConsume() throws InterruptedException {
        ArenaBruker bruker = new ArenaBruker();
        bruker.setAktoerid("1234");
        bruker.setFormidlingsgruppekode("ISERV");
        bruker.setIserv_fra_dato("2018-07-29T12:21:04.963+02:00");

        template.sendDefault(bruker.toString());

        ArenaBruker arenaBruker = consumer.deserialisereBruker(bruker.toString());
        assertThat(arenaBruker.getAktoerid().equals(bruker.getAktoerid()));
    }

}
