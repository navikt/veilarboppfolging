package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.sbl.jdbc.Database;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingRepositoryKvpTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "2222";
    private static final String ENHET = "1234";
    private static final String SAKSBEHANDLER = "4321";
    private static final String BEGRUNNELSE = "begrunnelse";

    @Mock
    private PepClient pepClientMock;

    private Database db = getBean(Database.class);
    private KvpRepository kvpRepository = new KvpRepository(db);

    @InjectMocks
    private OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(db);

    @Test
    public void test_eskaleringsvarsel_i_kvp_ingen_tilgang() throws Exception {
        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);

        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNull();
    }

    @Test
    public void test_eskaleringsvarsel_i_kvp_med_tilgang() throws Exception {
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);
        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);

        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNotNull();

    }

    @Test
    public void test_eskaleringsvarsel_uten_kvp() throws Exception {
        gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(AKTOR_ID);

        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        System.out.println(oppfolging);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNotNull();
    }


    private void gitt_oppfolging_med_aktiv_kvp_og_eskalering(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        kvpRepository.startKvp(AKTOR_ID, ENHET, SAKSBEHANDLER, BEGRUNNELSE);
        oppfolgingRepository.startEskalering(AKTOR_ID, SAKSBEHANDLER, BEGRUNNELSE, 0);
    }

    private void gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolgingRepository.startEskalering(AKTOR_ID, SAKSBEHANDLER, BEGRUNNELSE, 0);
    }

}
