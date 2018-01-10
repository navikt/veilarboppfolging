package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.Tilordning;
import no.nav.sbl.jdbc.Database;
import no.nav.fo.veilarboppfolging.services.EnhetPepClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    @BeforeAll
    public static void setup() throws IOException {
        annotationConfigApplicationContext.register(OppfolgingRepository.class);
        annotationConfigApplicationContext.register(EnhetPepClient.class);
    }

    private VeilederTilordningerRepository repository = new VeilederTilordningerRepository(
            getBean(Database.class),
            getBean(OppfolgingRepository.class)
    );

    private JdbcTemplate db = getBean(JdbcTemplate.class);

    @Test
    public void skalLeggeTilBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is("***REMOVED***"));
        Optional<Tilordning> ***REMOVED*** = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(***REMOVED***.map(Tilordning::isNyForVeileder).get(), is(true));
    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
        db.execute("INSERT INTO OPPFOLGINGSTATUS (aktor_id, oppdatert, under_oppfolging) " +
                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");

        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("0"));
        repository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("1"));

    }

    @Test
    public void skalSetteOppfolgingsflaggVedInsert() {
        repository.upsertVeilederTilordning("1111111", "***REMOVED***");
        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("1"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";

        repository.upsertVeilederTilordning(aktoerid, "***REMOVED***");
        repository.upsertVeilederTilordning(aktoerid, "***REMOVED***");

        assertThat(repository.hentTilordningForAktoer(aktoerid), is("***REMOVED***"));
    }

    @Test
    void kanMarkeresSomLest() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        repository.markerSomLestAvVeileder(AKTOR_ID);
        Optional<Tilordning> ***REMOVED*** = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(***REMOVED***.map(Tilordning::isNyForVeileder).get(), is(false));
    }

    @Test
    void blirNyVedNVeileder() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        repository.markerSomLestAvVeileder(AKTOR_ID);
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        Optional<Tilordning> ***REMOVED*** = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(***REMOVED***.map(Tilordning::isNyForVeileder).get(), is(true));
    }


}