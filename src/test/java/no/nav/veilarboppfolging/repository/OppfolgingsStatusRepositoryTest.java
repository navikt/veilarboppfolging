package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class OppfolgingsStatusRepositoryTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(jdbcTemplate);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void hentUnikeBrukerePage__skal_hente_page_med_unike_brukere() {
        AktorId aktorId1 = AktorId.of("aktorId1");
        AktorId aktorId2 = AktorId.of("aktorId2");
        AktorId aktorId3 = AktorId.of("aktorId3");

        oppfolgingsStatusRepository.opprettOppfolging(aktorId1);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId3);

        List<AktorId> unikeBrukerePage1 = oppfolgingsStatusRepository.hentUnikeBrukerePage(0, 1);
        assertEquals(1, unikeBrukerePage1.size());
        assertEquals(aktorId1, unikeBrukerePage1.get(0));

        List<AktorId> unikeBrukerePage2 = oppfolgingsStatusRepository.hentUnikeBrukerePage(1, 2);
        assertEquals(2, unikeBrukerePage2.size());
        assertEquals(aktorId2, unikeBrukerePage2.get(0));
        assertEquals(aktorId3, unikeBrukerePage2.get(1));
    }

}
