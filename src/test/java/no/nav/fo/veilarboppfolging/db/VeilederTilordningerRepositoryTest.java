package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.Tilordning;
import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    public static final String VEILEDER = "4321";
    public static final String OTHER_VEILEDER = "5432";

    @BeforeAll
    public static void setup() throws IOException {
        annotationConfigApplicationContext.register(OppfolgingRepository.class);
        annotationConfigApplicationContext.registerBean(PepClient.class, () -> Mockito.mock(PepClient.class));
    }

    private VeilederTilordningerRepository repository = new VeilederTilordningerRepository(
            getBean(Database.class),
            getBean(OppfolgingRepository.class)
    );

    private JdbcTemplate db = getBean(JdbcTemplate.class);

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
