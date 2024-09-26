package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningHistorikkEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class VeilederHistorikkRepositoryTest {
    private static final AktorId AKTOR_ID = AktorId.of(randomNumeric(10));
    private static final String VEILEDER1 = "Veileder1";
    private static final String VEILEDER2 = "Veileder2";
    private static final String TILORDNET_AV_VEILEDER1 = "TilordnetAvVeileder1";
    private static final String TILORDNET_AV_VEILEDER2 = "TilordnetAvVeileder2";

    private VeilederHistorikkRepository veilederHistorikkRepository = new VeilederHistorikkRepository(LocalDatabaseSingleton.INSTANCE.getJdbcTemplate());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skalInserteTilordnetVeileder () {
        veilederHistorikkRepository.insertTilordnetVeilederForAktorId(AKTOR_ID, VEILEDER1, TILORDNET_AV_VEILEDER1);
        veilederHistorikkRepository.insertTilordnetVeilederForAktorId(AKTOR_ID, VEILEDER2, TILORDNET_AV_VEILEDER2);
        List<VeilederTilordningHistorikkEntity> veilederHistorikk = veilederHistorikkRepository.hentTilordnedeVeiledereForAktorId(AKTOR_ID);
        assertThat(veilederHistorikk.size(), equalTo(2));
        assertThat(veilederHistorikk.get(0).getVeileder(), equalTo(VEILEDER2));
        assertThat(veilederHistorikk.get(1).getVeileder(), equalTo(VEILEDER1));
        assertThat(veilederHistorikk.get(0).getTilordnetAvVeileder(), equalTo(TILORDNET_AV_VEILEDER2));
        assertThat(veilederHistorikk.get(1).getTilordnetAvVeileder(), equalTo(TILORDNET_AV_VEILEDER1));
    }

}
