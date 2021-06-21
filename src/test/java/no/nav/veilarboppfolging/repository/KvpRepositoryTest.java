package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class KvpRepositoryTest extends IsolatedDatabaseTest {

    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        kvpRepository = new KvpRepository(db, transactor);
    }

    @Test
    public void stopKvp() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();
        stop_kvp();

        assertThat(hentGjeldendeKvp(AKTOR_ID), nullValue());
    }

    @Test
    public void hentKvpHistorikk() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();
        stop_kvp();
        start_kvp();
        stop_kvp();

        assertThat(kvpRepository.hentKvpHistorikk(AKTOR_ID), hasSize(2));
    }

    /**
     * Test that the serial field is incremented when a record is started and stopped.
     */
    @Test
    public void testSerial() {
        Kvp kvp;
        long serial;

        gittOppfolgingForAktor(AKTOR_ID);

        start_kvp();
        kvp = hentGjeldendeKvp(AKTOR_ID);
        serial = kvp.getSerial();

        stop_kvp();
        kvp = kvpRepository.fetch(kvp.getKvpId());
        assertThat(kvp.getSerial(), is(serial + 1));
    }

    private void stop_kvp() {
        long kvpId = kvpRepository.gjeldendeKvp(AKTOR_ID);
        kvpRepository.stopKvp(kvpId, AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE, NAV, ZonedDateTime.now());
    }

    private void start_kvp() {
        kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE, ZonedDateTime.now());
    }

    private Kvp hentGjeldendeKvp(AktorId aktorId) {
        long kvpId = oppfolgingsStatusRepository.fetch(aktorId).getGjeldendeKvpId();
        return kvpRepository.fetch(kvpId);
    }

    private void gittOppfolgingForAktor(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
    }
}
