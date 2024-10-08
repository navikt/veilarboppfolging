package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VeilederTilordningerRepositoryTest {

    private static final AktorId AKTOR_ID = AktorId.of(randomNumeric(10));
    private static final String VEILEDER = "4321";
    private static final String OTHER_VEILEDER = "5432";

    private VeilederTilordningerRepository veilederTilordningerRepository = new VeilederTilordningerRepository(LocalDatabaseSingleton.INSTANCE.getJdbcTemplate());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skalLeggeTilBruker() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        assertThat(veilederTilordningerRepository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        Optional<VeilederTilordningEntity> veileder = veilederTilordningerRepository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(VeilederTilordningEntity::isNyForVeileder).get(), is(true));
    }

    @Test
    public void skalOppdatereBrukerDersomDenFinnes() {
        AktorId aktoerid = AktorId.of("1111111");

        veilederTilordningerRepository.upsertVeilederTilordning(aktoerid, VEILEDER);
        veilederTilordningerRepository.upsertVeilederTilordning(aktoerid, OTHER_VEILEDER);

        assertThat(veilederTilordningerRepository.hentTilordningForAktoer(aktoerid), is(OTHER_VEILEDER));
    }

    @Test
    public void kanMarkeresSomLest() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        veilederTilordningerRepository.markerSomLestAvVeileder(AKTOR_ID);
        Optional<VeilederTilordningEntity> veileder = veilederTilordningerRepository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(VeilederTilordningEntity::isNyForVeileder).get(), is(false));
    }

    @Test
    public void blirNyVedNVeileder() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        veilederTilordningerRepository.markerSomLestAvVeileder(AKTOR_ID);
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, OTHER_VEILEDER);
        Optional<VeilederTilordningEntity> veileder = veilederTilordningerRepository.hentTilordnetVeileder(AKTOR_ID);
        assertThat(veileder.map(VeilederTilordningEntity::isNyForVeileder).get(), is(true));
    }


}
