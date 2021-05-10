package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class OppfolgingsperiodeRepositoryTest {

    private JdbcTemplate db = LocalH2Database.getDb();

    private OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Ignore // Kjører ikke på H2 med mindre man bytter ut ROWID med _rowid_
    @Test
    public void skal_hente_perioder_uten_uuid_og_sette_uuid() {
        String aktorId1 = "aktorid1";
        String aktorId2 = "aktorid2";

        oppfolgingsStatusRepository.opprettOppfolging(aktorId1);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2);

        db.update("INSERT INTO OPPFOLGINGSPERIODE(aktor_id, startDato, oppdatert) " +
                        "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", aktorId1);

        db.update("INSERT INTO OPPFOLGINGSPERIODE(aktor_id, startDato, oppdatert) " +
                "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", aktorId2);


        List<String> oppfolgingsperiodeRowIds1 = oppfolgingsPeriodeRepository.hentOppfolgingsPeriodeRowIdUtenUuid();

        assertEquals(2, oppfolgingsperiodeRowIds1.size());

        oppfolgingsPeriodeRepository.initialiserUuidPaOppfolgingsperiode(oppfolgingsperiodeRowIds1.get(0));

        List<String> oppfolgingsperiodeRowIds2 = oppfolgingsPeriodeRepository.hentOppfolgingsPeriodeRowIdUtenUuid();

        assertEquals(1, oppfolgingsperiodeRowIds2.size());
        assertEquals(oppfolgingsperiodeRowIds1.get(1), oppfolgingsperiodeRowIds2.get(0));
    }


}
