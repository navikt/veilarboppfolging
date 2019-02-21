package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.domain.Tilordning;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "2222";
    public static final String VEILEDER = "4321";
    public static final String OTHER_VEILEDER = "5432";


    @Inject
    private VeilederTilordningerRepository repository;

    @Inject
    private JdbcTemplate db;

    @Test
    public void skalLeggeTilBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        Optional<Tilordning> veileder = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(Tilordning::isNyForVeileder).get(), is(true));
    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
        db.execute("INSERT INTO OPPFOLGINGSTATUS (aktor_id, oppdatert, under_oppfolging) " +
                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");

        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("0"));
        repository.upsertVeilederTilordning("1111111", VEILEDER);
        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("1"));

    }

    @Test
    public void skalSetteOppfolgingsflaggVedInsert() {
        repository.upsertVeilederTilordning("1111111", VEILEDER);
        assertThat(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString(), is("1"));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        String aktoerid = "1111111";

        repository.upsertVeilederTilordning(aktoerid, VEILEDER);
        repository.upsertVeilederTilordning(aktoerid, OTHER_VEILEDER);

        assertThat(repository.hentTilordningForAktoer(aktoerid), is(OTHER_VEILEDER));
    }

    @Test
    void kanMarkeresSomLest() {
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        repository.markerSomLestAvVeileder(AKTOR_ID);
        Optional<Tilordning> veileder = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(Tilordning::isNyForVeileder).get(), is(false));
    }

    @Test
    void blirNyVedNVeileder() {
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        repository.markerSomLestAvVeileder(AKTOR_ID);
        repository.upsertVeilederTilordning(AKTOR_ID, OTHER_VEILEDER);
        Optional<Tilordning> veileder = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(Tilordning::isNyForVeileder).get(), is(true));
    }


}
