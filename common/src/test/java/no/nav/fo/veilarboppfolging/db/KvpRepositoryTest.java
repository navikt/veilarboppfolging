package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.feil.Feil;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.services.EnhetPepClient;
import no.nav.sbl.jdbc.Database;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KvpRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    private static final String SAKSBEHANDLER_ID = "Z990000";
    public static final String BEGRUNNELSE = "Begrunnelse";

    private Database db = getBean(Database.class);

    @Mock
    private EnhetPepClient enhetPepClientMock;

    @InjectMocks
    private OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(db);

    private KvpRepository kvpRepository = new KvpRepository(db);

    @Test
    public void startKvp() {
        when(enhetPepClientMock.harTilgang(anyString())).thenReturn(true);
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();

        assertThat(hentGjeldendeKvp(AKTOR_ID).getOpprettetBegrunnelse(), is(BEGRUNNELSE));

        when(enhetPepClientMock.harTilgang(anyString())).thenReturn(false);
        assertNull(hentGjeldendeKvp(AKTOR_ID));

        // Test that starting KVP an additional time yields an error.
        assertThrows(Feil.class, this::start_kvp);
    }

    @Test
    public void stopKvp() {
        gittOppfolgingForAktor(AKTOR_ID);
        start_kvp();
        stop_kvp();

        assertThat(hentGjeldendeKvp(AKTOR_ID), nullValue());
    }

    @Test
    public void stopKvpWithoutPeriod() {
        gittOppfolgingForAktor(AKTOR_ID);
        assertThrows(Feil.class, this::stop_kvp);
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

        when(enhetPepClientMock.harTilgang(anyString())).thenReturn(true);
        gittOppfolgingForAktor(AKTOR_ID);

        start_kvp();
        kvp = hentGjeldendeKvp(AKTOR_ID);
        serial = kvp.getSerial();

        stop_kvp();
        kvp = kvpRepository.fetch(kvp.getKvpId());
        assertThat(kvp.getSerial(), is(serial+1));
    }

    private void stop_kvp() {
        kvpRepository.stopKvp(AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE);
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
