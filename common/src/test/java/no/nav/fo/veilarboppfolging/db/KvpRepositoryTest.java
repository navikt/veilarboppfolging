package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.feil.Feil;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.Assert.assertThat;

public class KvpRepositoryTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    private static final String SAKSBEHANDLER_ID = "Z990000";
    public static final String BEGRUNNELSE = "Begrunnelse";

    private Database db = getBean(Database.class);

    private OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(db);

    private KvpRepository kvpRepository = new KvpRepository(db);

    @Nested
    class kvp {

        @Test
        public void startKvp() {
            gittOppfolgingForAktor(AKTOR_ID);
            start_kvp();

            assertThat(hentGjeldendeKvp(AKTOR_ID).getOpprettetBegrunnelse(), is(BEGRUNNELSE));

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

        private void stop_kvp() {
            kvpRepository.stopKvp(AKTOR_ID, SAKSBEHANDLER_ID, BEGRUNNELSE);
        }

        private void start_kvp() {
            kvpRepository.startKvp(AKTOR_ID, "0123", SAKSBEHANDLER_ID, BEGRUNNELSE);
        }

        private Kvp hentGjeldendeKvp(String aktorId) {
            return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeKvp();
        }
    }

    private Oppfolging gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }
}
