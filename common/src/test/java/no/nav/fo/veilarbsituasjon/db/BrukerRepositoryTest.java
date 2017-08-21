package no.nav.fo.veilarbsituasjon.db;

import no.nav.fo.IntegrasjonsTest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BrukerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    private BrukerRepository brukerRepository = new BrukerRepository(getBean(JdbcTemplate.class));

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    @Test
    public void skalLeggeTilBruker() {
        brukerRepository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        assertThat(brukerRepository.hentTilordningForAktoer(AKTOR_ID).getVeileder(), is("***REMOVED***"));
    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
        db.execute("INSERT INTO SITUASJON (AKTORID, OPPDATERT, OPPFOLGING) " +
                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");

        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("0"));
        brukerRepository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("1"));

    }

    @Test
    public void skalSetteOppfolgingsflaggVedInsert() {
        brukerRepository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("1"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";

        brukerRepository.upsertVeilederTilordning(aktoerid, "***REMOVED***");
        brukerRepository.upsertVeilederTilordning(aktoerid, "***REMOVED***");

        assertThat(brukerRepository.hentTilordningForAktoer(aktoerid).getVeileder(), is("***REMOVED***"));
    }

}