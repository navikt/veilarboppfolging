package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class ProducerKafkaTest extends KafkaTest {

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;
    private JdbcTemplate db;
    private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    @Before
    public void setup() {
        db = new JdbcTemplate(setupInMemoryDatabase());
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(db);
        avsluttOppfolgingProducer = new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, "HELLO_WORLD_TOPIC");
    }

    @Test
    public void slett_avsluttbruker_fra_db_ved_vedlykkedSendning() {
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker("1234");
        List<AvsluttOppfolgingKafkaDTO> alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
        assertThat(alleBrukere.size()).isEqualTo(1);
        avsluttOppfolgingProducer.avsluttOppfolgingEvent("1234", new Date());

        boolean prosessert = false;
        while(!prosessert) {
            try {
                Thread.sleep(10);
                prosessert = true;
                alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
                assertThat(alleBrukere.size()).isEqualTo(0);
            } catch(Throwable a) {
            }
        }
    }

    @Test
    public void gitt_att_misslyckades_produsere_kafka_meldig_skall_ikke_slette_nyare_inslag_i_db() {
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker("1234");
        long DAG_I_MILLISEK = 1000 * 60 * 60 * 24;
        avsluttOppfolgingProducer.avsluttOppfolgingEvent("1234", new Date(System.currentTimeMillis() - (7 * DAG_I_MILLISEK)));

        boolean prosessert = false;
        while(!prosessert) {
            try {
                Thread.sleep(10);
                prosessert = true;
                List<AvsluttOppfolgingKafkaDTO> alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
                assertThat(alleBrukere.size()).isEqualTo(1);
            } catch(Throwable a) {
            }
        }
    }


}
