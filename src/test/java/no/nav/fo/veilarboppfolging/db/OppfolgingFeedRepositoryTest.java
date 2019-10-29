package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class OppfolgingFeedRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";

    @Inject
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Inject
    private OppfolgingFeedRepository feedRepository;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Ignore //enable igjen etter at denne appen er paa naiserator/laptop
    @Test
    public void skalHenteBrukere() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        assertThat(veilederTilordningerRepository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(1));
        assertThat(oppfolgingFeedDTOS.get(0).isNyForVeileder(), is(true));
        assertThat(oppfolgingFeedDTOS.get(0).getVeileder(), is(VEILEDER));
    }

    @Ignore //enable igjen etter at denne appen er paa naiserator/laptop
    @Test
    public void skalHenteMaxBruker() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        veilederTilordningerRepository.upsertVeilederTilordning("1111", VEILEDER);
        veilederTilordningerRepository.upsertVeilederTilordning("3333", VEILEDER);
        veilederTilordningerRepository.upsertVeilederTilordning("4444", VEILEDER);

        assertThat(veilederTilordningerRepository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        assertThat(oppfolgingFeedDTOS.size(), is(2));
    }

    @Ignore //enable igjen etter at denne appen er paa naiserator/laptop
    @Test
    public void skal_hente_ut_bruker_med_sluttdato_ut_paa_feed() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        List<OppfolgingFeedDTO> feedElementer = feedRepository.hentEndringerEtterTimestamp(new Timestamp(0), 2);
        oppfolgingRepository.avsluttOppfolging(AKTOR_ID, VEILEDER, "test");
        assertThat(feedElementer.size(), is(1));
        assertThat(feedElementer.get(0).getAktoerid(), is(AKTOR_ID));
    }

}
