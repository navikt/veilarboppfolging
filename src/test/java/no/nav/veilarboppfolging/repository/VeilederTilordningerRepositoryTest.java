package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.Tilordning;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest {

    private static final String AKTOR_ID = "2222";
    public static final String VEILEDER = "4321";
    public static final String OTHER_VEILEDER = "5432";

    private VeilederTilordningerRepository repository = new VeilederTilordningerRepository(LocalH2Database.getDb());

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skalLeggeTilBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        Optional<Tilordning> veileder = repository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(Tilordning::isNyForVeileder).get(), is(true));
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
