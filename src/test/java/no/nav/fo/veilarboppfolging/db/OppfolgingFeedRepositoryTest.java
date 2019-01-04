package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.sbl.jdbc.Database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class OppfolgingFeedRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";

    @BeforeAll
    public static void setup() {
        annotationConfigApplicationContext.register(OppfolgingRepository.class);
        annotationConfigApplicationContext.registerBean(PepClient.class, () -> Mockito.mock(PepClient.class));
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
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(1));
        assertThat(oppfolgingFeedDTOS.get(0).isNyForVeileder(), is(true));
        assertThat(oppfolgingFeedDTOS.get(0).getVeileder(), is(VEILEDER));
    }


    @Test
    public void skalHenteMaxBruker() {
        repository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        repository.upsertVeilederTilordning("1111", VEILEDER);
        repository.upsertVeilederTilordning("3333", VEILEDER);
        repository.upsertVeilederTilordning("4444", VEILEDER);

        assertThat(repository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(2));
    }
}
