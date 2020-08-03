package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EskaleringsvarselRepositoryTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";
    private static final int NUM_ITEMS = 10;

    private OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(LocalH2Database.getDb());

    private EskaleringsvarselRepository eskaleringsvarselRepository = new EskaleringsvarselRepository(LocalH2Database.getDb());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    /**
     * Test that creating an escalation warning inserts a record in the database,
     * and creates a connection to the OPPFOLGING table as well.
     */
    @Test
    public void testCreateAndFinish() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        // Create the escalation warning, and test that retrieving
        // the current warning yields the object we just created.
        eskaleringsvarselRepository.create(EskaleringsvarselData.builder()
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
        eskaleringsvarselRepository.finish(AKTOR_ID, e.getVarselId(), SAKSBEHANDLER_ID, "Begrunnelse");

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
            eskaleringsvarselRepository.create(e);
        }

        list = eskaleringsvarselRepository.history(AKTOR_ID);
        assertEquals(list.size(), NUM_ITEMS);
    }

    private EskaleringsvarselData gjeldendeEskaleringsVarsel(String aktorId) {
        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);
        return eskaleringsvarselRepository.fetch(oppfolging.getGjeldendeEskaleringsvarselId());
    }

}

