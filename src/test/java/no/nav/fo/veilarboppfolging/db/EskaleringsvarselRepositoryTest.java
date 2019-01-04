package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.sbl.jdbc.Database;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EskaleringsvarselRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "2222";
    private static final String SAKSBEHANDLER_ID = "Z990000";
    private static final String BEGRUNNELSE = "Begrunnelse";
    private static final int NUM_ITEMS = 10;

    private Database db = getBean(Database.class);

    private OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(db);

    private EskaleringsvarselRepository repository = new EskaleringsvarselRepository(db);

    /**
     * Test that creating an escalation warning inserts a record in the database,
     * and creates a connection to the OPPFOLGING table as well.
     */
    @Test
    public void testCreateAndFinish() {
        gittOppfolgingForAktor(AKTOR_ID);

        EskaleringsvarselData e = EskaleringsvarselData.builder()
                .aktorId(AKTOR_ID)
                .opprettetAv(SAKSBEHANDLER_ID)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build();

        // Create the escalation warning, and test that retrieving
        // the current warning yields the object we just created.
        repository.create(e);

        e = gjeldendeEskaleringsVarsel(AKTOR_ID);

        assertThat(e.getAktorId(), is(AKTOR_ID));
        assertThat(e.getOpprettetAv(), is(SAKSBEHANDLER_ID));
        assertThat(e.getOpprettetBegrunnelse(), is(BEGRUNNELSE));

        // Finish the escalation warning, and test that retrieving
        // the current warning yields nothing.
        repository.finish(e);

        e = gjeldendeEskaleringsVarsel(AKTOR_ID);
        assertNull(e);
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

