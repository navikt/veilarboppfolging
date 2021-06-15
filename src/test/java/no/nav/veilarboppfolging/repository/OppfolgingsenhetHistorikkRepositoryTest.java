package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class OppfolgingsenhetHistorikkRepositoryTest {

    private static final AktorId AKTOR_ID = AktorId.of(randomNumeric(10));

    private static OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository = new OppfolgingsenhetHistorikkRepository(LocalH2Database.getDb());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skal_inserte_og_hente_ut_endringer_paa_enhet_som_er_sortert() {
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "5");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "4");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "3");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "2");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "1");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "0");

        List<OppfolgingsenhetEndringData> enhetsHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(enhetsHistorikk.size(), equalTo(6));

        assertEquals("0", enhetsHistorikk.get(0).getEnhet());
        assertEquals("1", enhetsHistorikk.get(1).getEnhet());
        assertEquals("2", enhetsHistorikk.get(2).getEnhet());
        assertEquals("3", enhetsHistorikk.get(3).getEnhet());
        assertEquals("4", enhetsHistorikk.get(4).getEnhet());
        assertEquals("5", enhetsHistorikk.get(5).getEnhet());
    }
}
