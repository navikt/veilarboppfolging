package no.nav.veilarboppfolging.repository;

import lombok.val;
import no.nav.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingRepositoryKvpTest extends IsolatedDatabaseTest {

    private static final String AKTOR_ID = randomNumeric(10);
    private static final String ENHET = "1234";
    private static final String SAKSBEHANDLER = "4321";
    private static final String BEGRUNNELSE = "begrunnelse";

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    private EskaleringsvarselRepository eskaleringsvarselRepository;

    @Before
    public void setup() {
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        eskaleringsvarselRepository = new EskaleringsvarselRepository(db);
        kvpRepository = new KvpRepository(db);
    }

    // TODO: tilgangskontroll skal ikke gjøres så nært databasen, flytt til en service
//    @Test
//    public void test_eskaleringsvarsel_i_kvp_ingen_tilgang() {
//        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);
//
//        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(AKTOR_ID);
//        EskaleringsvarselData eskaleringsvarsel = eskaleringsvarselRepository.fetch(oppfolging.getGjeldendeEskaleringsvarselId());
//
//        assertThat(eskaleringsvarsel).isNull();
//    }

    @Test
    public void test_eskaleringsvarsel_i_kvp_med_tilgang() {
        gitt_oppfolging_med_aktiv_kvp_og_eskalering(AKTOR_ID);

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(AKTOR_ID);
        EskaleringsvarselData eskaleringsvarsel = eskaleringsvarselRepository.fetch(oppfolging.getGjeldendeEskaleringsvarselId());

        assertThat(eskaleringsvarsel).isNotNull();
    }

    @Test
    public void test_eskaleringsvarsel_uten_kvp() {
        gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(AKTOR_ID);

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(AKTOR_ID);
        EskaleringsvarselData eskaleringsvarsel = eskaleringsvarselRepository.fetch(oppfolging.getGjeldendeEskaleringsvarselId());

        assertThat(eskaleringsvarsel).isNotNull();
    }


    private void gitt_oppfolging_med_aktiv_kvp_og_eskalering(String aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        kvpRepository.startKvp(AKTOR_ID, ENHET, SAKSBEHANDLER, BEGRUNNELSE);
        startEskalering();
    }

    private void gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(String aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        startEskalering();
    }

    private void startEskalering() {
        val e = EskaleringsvarselData.builder()
                .aktorId(AKTOR_ID)
                .opprettetAv(SAKSBEHANDLER)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .tilhorendeDialogId(0)
                .build();

        eskaleringsvarselRepository.create(e);
    }

}
