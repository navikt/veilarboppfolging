package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KvpRepositoryTest extends IsolatedDatabaseTest {

    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(db));
        kvpRepository = new KvpRepository(db, namedParameterJdbcTemplate, transactor);
    }

    @Test
    public void hentGjeldendeKvpPeriode__skal_hente_gjeldende_periode() {
        gittOppfolgingForAktor(AKTOR_ID);

        kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE, ZonedDateTime.now().minusSeconds(5));

        stop_kvp();

        kvpRepository.startKvp(AKTOR_ID, "5678", SAKSBEHANDLER_ID, BEGRUNNELSE, ZonedDateTime.now());

        Optional<KvpPeriodeEntity> maybeGjeldendePeriode = kvpRepository.hentGjeldendeKvpPeriode(AKTOR_ID);

        assertTrue(maybeGjeldendePeriode.isPresent());
        assertEquals("5678", maybeGjeldendePeriode.get().getEnhet());
    }

    @Test
    public void hentGjeldendeKvpPeriode__skal_hente_gjeldende_periode_med_flere_perioder() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();

        Optional<KvpPeriodeEntity> maybeGjeldendePeriode = kvpRepository.hentGjeldendeKvpPeriode(AKTOR_ID);

        assertTrue(maybeGjeldendePeriode.isPresent());
    }

    @Test
    public void hentGjeldendeKvpPeriode__skal_returnere_empty_hvis_ingen_gjeldende_periode() {
        Optional<KvpPeriodeEntity> maybeGjeldendePeriode = kvpRepository.hentGjeldendeKvpPeriode(AKTOR_ID);

        assertTrue(maybeGjeldendePeriode.isEmpty());
    }

    @Test
    public void stopKvp() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();
        stop_kvp();

        assertTrue(hentGjeldendeKvp(AKTOR_ID).isEmpty());
    }

    @Test
    public void hentKvpHistorikk() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();
        stop_kvp();
        start_kvp();
        stop_kvp();

        assertEquals(2, kvpRepository.hentKvpHistorikk(AKTOR_ID).size());
    }

    /**
     * Test that the serial field is incremented when a record is started and stopped.
     */
    @Test
    public void testSerial() {
        long serial;

        gittOppfolgingForAktor(AKTOR_ID);

        start_kvp();
        KvpPeriodeEntity kvp = hentGjeldendeKvp(AKTOR_ID).orElseThrow();
        serial = kvp.getSerial();

        stop_kvp();
        var maybeKvp = kvpRepository.hentKvpPeriode(kvp.getKvpId());
        assertTrue(maybeKvp.isPresent());
        assertEquals(serial + 1, maybeKvp.get().getSerial());
    }

    private void stop_kvp() {
        long kvpId = kvpRepository.gjeldendeKvp(AKTOR_ID);
        kvpRepository.stopKvp(kvpId, AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE, NAV, ZonedDateTime.now());
    }

    private void start_kvp() {
        kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE, ZonedDateTime.now());
    }

    private Optional<KvpPeriodeEntity> hentGjeldendeKvp(AktorId aktorId) {
        long kvpId = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElseThrow().getGjeldendeKvpId();
        return kvpRepository.hentKvpPeriode(kvpId);
    }

    private void gittOppfolgingForAktor(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
    }
}
