package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.service.OppfolgingRepositoryService;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingRepositoryKvpTest {

    private static final String AKTOR_ID = "2222";
    private static final String ENHET = "1234";
    private static final String SAKSBEHANDLER = "4321";
    private static final String BEGRUNNELSE = "begrunnelse";

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    private OppfolgingRepositoryService oppfolgingRepositoryService;

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void test_eskaleringsvarsel_i_kvp_ingen_tilgang() throws Exception {
        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);

        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNull();
    }

    @Test
    public void test_eskaleringsvarsel_i_kvp_med_tilgang() throws Exception {
        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);

        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNotNull();

    }

    @Test
    public void test_eskaleringsvarsel_uten_kvp() throws Exception {
        gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepositoryService.hentOppfolging(AKTOR_ID).orElseThrow(Exception::new);
        assertThat(oppfolging.getGjeldendeEskaleringsvarsel()).isNotNull();
    }


    private void gitt_oppfolging_med_aktiv_kvp_og_eskalering(String aktorId) {
        oppfolgingRepositoryService.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingsStatusRepository.opprettOppfolging(aktorId));
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        kvpRepository.startKvp(AKTOR_ID, ENHET, SAKSBEHANDLER, BEGRUNNELSE);
        oppfolgingRepositoryService.startEskalering(AKTOR_ID, SAKSBEHANDLER, BEGRUNNELSE, 0);
    }

    private void gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(String aktorId) {
        oppfolgingRepositoryService.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingsStatusRepository.opprettOppfolging(aktorId));
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolgingRepositoryService.startEskalering(AKTOR_ID, SAKSBEHANDLER, BEGRUNNELSE, 0);
    }

}
