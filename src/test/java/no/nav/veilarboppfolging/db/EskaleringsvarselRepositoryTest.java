package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.test.DatabaseTest;
import no.nav.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.veilarboppfolging.domain.Oppfolging;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EskaleringsvarselRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";
    private static final int NUM_ITEMS = 10;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Inject
    private EskaleringsvarselRepository repository;

    /**
     * Test that creating an escalation warning inserts a record in the database,
     * and creates a connection to the OPPFOLGING table as well.
     */
    @Test
    public void testCreateAndFinish() {
        gittOppfolgingForAktor(AKTOR_ID);

        // Create the escalation warning, and test that retrieving
        // the current warning yields the object we just created.
        repository.create(EskaleringsvarselData.builder()
                .aktorId(AKTOR_ID)
                .opprettetAv(SAKSBEHANDLER_ID)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build());

        EskaleringsvarselData e = gjeldendeEskaleringsVarsel(AKTOR_ID);

        assertThat(e.getAktorId(), is(AKTOR_ID));
        assertThat(e.getOpprettetAv(), is(SAKSBEHANDLER_ID));
        assertThat(e.getOpprettetBegrunnelse(), is(BEGRUNNELSE));

        // Finish the escalation warning, and test that retrieving
        // the current warning yields nothing.
        repository.finish(AKTOR_ID, e.getVarselId(), SAKSBEHANDLER_ID, "Begrunnelse");

        assertNull(gjeldendeEskaleringsVarsel(AKTOR_ID));
    }

    /**
     * Create a series of escalation warnings, and test that the correct entries
     * are returned when retrieving the list back from the database.
     */
    @Test
    public void testHistory() {
        List<EskaleringsvarselData> list;
        EskaleringsvarselData e;

        for (int i = 0; i < NUM_ITEMS; i++) {
            e = EskaleringsvarselData.builder()
                    .aktorId(AKTOR_ID)
                    .opprettetAv(SAKSBEHANDLER_ID)
                    .opprettetBegrunnelse(BEGRUNNELSE)
                    .build();
            repository.create(e);
        }

        list = oppfolgingRepository.hentEskaleringhistorikk(AKTOR_ID);
        assertEquals(list.size(), NUM_ITEMS);
    }

    private EskaleringsvarselData gjeldendeEskaleringsVarsel(String aktorId) {
        return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeEskaleringsvarsel();
    }

    private void gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
    }
}

