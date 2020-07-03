package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.domain.VeilederTilordningerData;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;


public class VeilederHistorikkRepositoryTest {
    private static VeilederHistorikkRepository veilederHistorikkRepository;
    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER1 = "Veileder1";
    private static final String VEILEDER2 = "Veileder2";

    @Before
    public void setup() {
        veilederHistorikkRepository = new VeilederHistorikkRepository(LocalH2Database.getDb());
    }

    @Test
    public void skalInserteTilordnetVeileder () {
        veilederHistorikkRepository.insertTilordnetVeilederForAktorId(AKTOR_ID, VEILEDER1);
        veilederHistorikkRepository.insertTilordnetVeilederForAktorId(AKTOR_ID, VEILEDER2);
        List<VeilederTilordningerData> veilederHistorikk = veilederHistorikkRepository.hentTilordnedeVeiledereForAktorId(AKTOR_ID);
        assertThat(veilederHistorikk.size(), equalTo(2));
        assertThat(veilederHistorikk.get(0).getVeileder(), equalTo(VEILEDER2));
        assertThat(veilederHistorikk.get(1).getVeileder(), equalTo(VEILEDER1));
    }

}
