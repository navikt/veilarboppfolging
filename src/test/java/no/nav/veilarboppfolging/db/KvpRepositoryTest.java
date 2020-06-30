package no.nav.veilarboppfolging.db;

import no.nav.apiapp.security.PepClient;
import no.nav.veilarboppfolging.test.DatabaseTest;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.jdbc.Database;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class KvpRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String SAKSBEHANDLER_ID = "saksbehandlerId";
    private static final String BEGRUNNELSE = "Begrunnelse";

    private Database db = getBean(Database.class);

    @Inject
    private PepClient pepClientMock;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    private KvpRepository kvpRepository = new KvpRepository(db);

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
    public void testSerial() throws PepException {
        Kvp kvp;
        long serial;

        when(pepClientMock.harTilgangTilEnhet(anyString())).thenReturn(true);
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
        kvpRepository.stopKvp(kvpId, AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE, NAV);
    }

    private void start_kvp() {
        kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE);
    }

    private Kvp hentGjeldendeKvp(String aktorId) {
        return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeKvp();
    }

    private Oppfolging gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }
}
