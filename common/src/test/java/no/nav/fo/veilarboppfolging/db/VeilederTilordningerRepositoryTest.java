package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.IntegrasjonsTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    @BeforeAll
    public static void setup() throws IOException {
        annotationConfigApplicationContext.register(OppfolgingRepository.class);
    }

    private VeilederTilordningerRepository repository = new VeilederTilordningerRepository(
            getBean(JdbcTemplate.class),
            getBean(OppfolgingRepository.class)
    );

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    @Test
    public void skalLeggeTilBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is("***REMOVED***"));
    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
        db.execute("INSERT INTO SITUASJON (AKTORID, OPPDATERT, OPPFOLGING) " +
                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");

        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("0"));
        repository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("1"));

    }

    @Test
    public void skalSetteOppfolgingsflaggVedInsert() {
        repository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM SITUASJON WHERE AKTORID = '1111111'").get(0).get("OPPFOLGING").toString(), is("1"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";

        repository.upsertVeilederTilordning(aktoerid, "***REMOVED***");
        repository.upsertVeilederTilordning(aktoerid, "***REMOVED***");

        assertThat(repository.hentTilordningForAktoer(aktoerid), is("***REMOVED***"));
    }

}