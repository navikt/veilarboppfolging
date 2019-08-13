package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class AvsluttOppfolgingRespositoryTest {
    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;
    private JdbcTemplate db;

    @Before
    public void setup() {
        db = new JdbcTemplate(setupInMemoryDatabase());
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(db);
    }

    @Test
    public void insert_og_slett_bruker_fra_avsluttOppfolgingEndringRepository() {
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker("1234");
        List<AvsluttOppfolgingKafkaDTO> alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
        assertThat(alleBrukere.size()).isEqualTo(1);

        AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaBruker = alleBrukere.get(0);
        avsluttOppfolgingEndringRepository.deleteAvsluttOppfolgingBruker(avsluttOppfolgingKafkaBruker.getAktorId(), avsluttOppfolgingKafkaBruker.getSluttdato());
        assertThat(avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere().size()).isEqualTo(0);
    }

}
