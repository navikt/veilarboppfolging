package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class OppfolgingsenhetHistorikkRepositoryTest {
    private static OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;
    private static final String AKTOR_ID = "11111";

    @Before
    public void setup() {
        oppfolgingsenhetHistorikkRepository = new OppfolgingsenhetHistorikkRepository(new JdbcTemplate(setupInMemoryDatabase()));
    }

    @Test
    public void skal_inserte_og_hente_ut_2_endringer_paa_enhet() {
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "0123");
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "3210");
        List<OppfolgingsenhetEndringData> enhetsHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(enhetsHistorikk.size(), equalTo(2));
    }
}