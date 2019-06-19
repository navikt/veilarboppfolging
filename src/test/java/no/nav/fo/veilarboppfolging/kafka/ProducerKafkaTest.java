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

    @Before
    public void setup() {
        db = new JdbcTemplate(setupInMemoryDatabase());
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(db);
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker("1234");
    }


    @Test
    public void skallSletteAvsluttBrukerVedVedlykkedSendning() {
        AvsluttOppfolgingProducer avsluttOppfolgingProducer  = new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, "HELLO_WORLD_TOPIC");
        avsluttOppfolgingProducer.avsluttOppfolgingEvent("1234", new Date());

        boolean prosessert = false;
        while(!prosessert) {
            try {
                Thread.sleep(10);
                List<AvsluttOppfolgingKafkaDTO> alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
                assertThat(alleBrukere.size()).isEqualTo(0);
                prosessert = true;
            } catch(Throwable a) {
            }
        }
    }


}
