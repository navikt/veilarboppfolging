package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.EskaleringsvarselEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EskaleringsvarselRepositoryTest {

    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";
    private static final int NUM_ITEMS = 10;

    private JdbcTemplate db = LocalH2Database.getDb();

    private TransactionTemplate transactor = DbTestUtils.createTransactor(db);

    private OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);

    private EskaleringsvarselRepository eskaleringsvarselRepository = new EskaleringsvarselRepository(db, transactor);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void hentGjeldendeEskaleringsvarsel__skal_hente_gjeldende_eskaleringsvarsel() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        eskaleringsvarselRepository.create(EskaleringsvarselEntity.builder()
                .aktorId(AKTOR_ID.get())
                .opprettetAv(SAKSBEHANDLER_ID)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build());

        var maybeEskaleringsvarsel = eskaleringsvarselRepository.hentGjeldendeEskaleringsvarsel(AKTOR_ID);

        assertTrue(maybeEskaleringsvarsel.isPresent());
    }

    @Test
    public void hentGjeldendeEskaleringsvarsel__skal_hente_gjeldende_eskaleringsvarsel_med_flere_tidligere_varsler() {
        String saksbehandler2 = "Z12345";

        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        eskaleringsvarselRepository.create(EskaleringsvarselEntity.builder()
                .aktorId(AKTOR_ID.get())
                .opprettetAv(SAKSBEHANDLER_ID)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build());

        eskaleringsvarselRepository.create(EskaleringsvarselEntity.builder()
                .aktorId(AKTOR_ID.get())
                .opprettetAv(saksbehandler2)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build());

        var maybeEskaleringsvarsel = eskaleringsvarselRepository.hentGjeldendeEskaleringsvarsel(AKTOR_ID);

        assertTrue(maybeEskaleringsvarsel.isPresent());
        assertEquals(saksbehandler2, maybeEskaleringsvarsel.get().getOpprettetAv());
    }

    @Test
    public void hentGjeldendeEskaleringsvarsel__skal_returnere_empty_hvis_ingen_gjeldende_eskaleringsvarsel() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        var maybeEskaleringsvarsel = eskaleringsvarselRepository.hentGjeldendeEskaleringsvarsel(AKTOR_ID);

        assertTrue(maybeEskaleringsvarsel.isEmpty());
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
        eskaleringsvarselRepository.create(EskaleringsvarselEntity.builder()
                .aktorId(AKTOR_ID.get())
                .opprettetAv(SAKSBEHANDLER_ID)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .build());

        Optional<EskaleringsvarselEntity> maybeEskaleringsvarsel1 = gjeldendeEskaleringsVarsel(AKTOR_ID);

        assertTrue(maybeEskaleringsvarsel1.isPresent());

        EskaleringsvarselEntity eskaleringsvarsel = maybeEskaleringsvarsel1.get();

        assertEquals(AKTOR_ID.get(), eskaleringsvarsel.getAktorId());
        assertEquals(SAKSBEHANDLER_ID, eskaleringsvarsel.getOpprettetAv());
        assertEquals(BEGRUNNELSE, eskaleringsvarsel.getOpprettetBegrunnelse());

        // Finish the escalation warning, and test that retrieving
        // the current warning yields nothing.
        eskaleringsvarselRepository.finish(AKTOR_ID, eskaleringsvarsel.getVarselId(), SAKSBEHANDLER_ID, "Begrunnelse", ZonedDateTime.now());

        Optional<EskaleringsvarselEntity> maybeIngenEskaleringsvarsel = gjeldendeEskaleringsVarsel(AKTOR_ID);

        assertTrue(maybeIngenEskaleringsvarsel.isEmpty());
    }

    /**
     * Create a series of escalation warnings, and test that the correct entries
     * are returned when retrieving the list back from the database.
     */
    @Test
    public void testHistory() {
        List<EskaleringsvarselEntity> list;
        EskaleringsvarselEntity e;

        for (int i = 0; i < NUM_ITEMS; i++) {
            e = EskaleringsvarselEntity.builder()
                    .aktorId(AKTOR_ID.get())
                    .opprettetAv(SAKSBEHANDLER_ID)
                    .opprettetBegrunnelse(BEGRUNNELSE)
                    .build();
            eskaleringsvarselRepository.create(e);
        }

        list = eskaleringsvarselRepository.history(AKTOR_ID);
        assertEquals(list.size(), NUM_ITEMS);
    }

    private Optional<EskaleringsvarselEntity> gjeldendeEskaleringsVarsel(AktorId aktorId) {
        OppfolgingEntity oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElseThrow();
        return eskaleringsvarselRepository.hentEskaleringsvarsel(oppfolging.getGjeldendeEskaleringsvarselId());
    }

}

