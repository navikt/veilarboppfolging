package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.IntegrasjonsTest;

import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.sbl.jdbc.Database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class OppfolgingFeedRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";

    @BeforeAll
    public static void setup() throws IOException {
        annotationConfigApplicationContext.register(OppfolgingRepository.class);
    }

    private VeilederTilordningerRepository repository = new VeilederTilordningerRepository(
            getBean(Database.class),
            getBean(OppfolgingRepository.class)
    );

    private OppfolgingFeedRepository feedRepository = new OppfolgingFeedRepository(
            getBean(JdbcTemplate.class),
            null);

    @Test
    public void skalHenteBrukere() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is("***REMOVED***"));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(1));
        assertThat(oppfolgingFeedDTOS.get(0).isNyForVeileder(), is(true));
        assertThat(oppfolgingFeedDTOS.get(0).getVeileder(), is("***REMOVED***"));
    }


    @Test
    public void skalHenteMaxBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, "***REMOVED***");
        repository.upsertVeilederTilordning("1111", "***REMOVED***");
        repository.upsertVeilederTilordning("3333", "***REMOVED***");
        repository.upsertVeilederTilordning("4444", "***REMOVED***");

        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is("***REMOVED***"));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(2));
    }
}