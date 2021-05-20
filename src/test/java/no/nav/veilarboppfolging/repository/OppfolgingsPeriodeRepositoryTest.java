package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class OppfolgingsPeriodeRepositoryTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(jdbcTemplate);

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(jdbcTemplate);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void hentUnikeBrukerePage__skal_hente_page_med_unike_brukere() {
        String aktorId1 = "aktorId1";
        String aktorId2 = "aktorId2";
        String aktorId3 = "aktorId3";

        oppfolgingsStatusRepository.opprettOppfolging(aktorId1);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId3);

        oppfolgingsPeriodeRepository.start(aktorId1);
        oppfolgingsPeriodeRepository.start(aktorId2);
        oppfolgingsPeriodeRepository.start(aktorId3);

        List<String> unikeBrukerePage1 = oppfolgingsPeriodeRepository.hentUnikeBrukerePage(0, 1);
        assertEquals(1, unikeBrukerePage1.size());
        assertEquals(aktorId1, unikeBrukerePage1.get(0));

        List<String> unikeBrukerePage2 = oppfolgingsPeriodeRepository.hentUnikeBrukerePage(1, 2);
        assertEquals(2, unikeBrukerePage2.size());
        assertEquals(aktorId2, unikeBrukerePage2.get(0));
        assertEquals(aktorId3, unikeBrukerePage2.get(1));
    }

}
