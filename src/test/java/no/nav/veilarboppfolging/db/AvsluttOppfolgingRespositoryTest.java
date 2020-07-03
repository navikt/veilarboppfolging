package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AvsluttOppfolgingRespositoryTest {

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Before
    public void setup() {
        avsluttOppfolgingEndringRepository = new AvsluttOppfolgingEndringRepository(LocalH2Database.getDb());
    }

    @Test
    public void insert_og_slett_bruker_fra_avsluttOppfolgingEndringRepository() {
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker("1234", LocalDateTime.now());
        List<AvsluttOppfolgingKafkaDTO> alleBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
        assertThat(alleBrukere.size()).isEqualTo(1);

        AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaBruker = alleBrukere.get(0);
        avsluttOppfolgingEndringRepository.deleteAvsluttOppfolgingBruker(avsluttOppfolgingKafkaBruker.getAktorId(), avsluttOppfolgingKafkaBruker.getSluttdato());
        assertThat(avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere().size()).isEqualTo(0);
    }

}
