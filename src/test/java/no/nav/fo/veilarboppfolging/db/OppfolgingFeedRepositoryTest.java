package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class OppfolgingFeedRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";
    private static final String VEILEDER2 = "4321";

    @Inject
    private VeilederTilordningerRepository repository;

    @Inject
    private OppfolgingFeedRepository feedRepository;

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
