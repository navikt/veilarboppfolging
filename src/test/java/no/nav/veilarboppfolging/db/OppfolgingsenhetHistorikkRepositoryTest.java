package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.test.DatabaseTest;
import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class OppfolgingsenhetHistorikkRepositoryTest extends DatabaseTest {
    private static OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;
    private static final String AKTOR_ID = "11111";

    @Before
    public void setup() {
        oppfolgingsenhetHistorikkRepository = new OppfolgingsenhetHistorikkRepository(new JdbcTemplate(setupInMemoryDatabase()));
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