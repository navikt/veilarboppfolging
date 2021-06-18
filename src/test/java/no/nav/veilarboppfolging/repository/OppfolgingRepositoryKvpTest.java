package no.nav.veilarboppfolging.repository;

import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingRepositoryKvpTest extends IsolatedDatabaseTest {

    private static final AktorId AKTOR_ID = AktorId.of(randomNumeric(10));
    private static final String ENHET = "1234";
    private static final String SAKSBEHANDLER = "4321";
    private static final String BEGRUNNELSE = "begrunnelse";

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private KvpRepository kvpRepository;

    private EskaleringsvarselRepository eskaleringsvarselRepository;

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        eskaleringsvarselRepository = new EskaleringsvarselRepository(db, transactor);
        kvpRepository = new KvpRepository(db, transactor);
    }

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

    private void gitt_oppfolging_med_aktiv_kvp_og_eskalering(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        kvpRepository.startKvp(AKTOR_ID, ENHET, SAKSBEHANDLER, BEGRUNNELSE);
        startEskalering();
    }

    private void gitt_oppfolging_uten_aktiv_kvp_men_med_eskalering(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        startEskalering();
    }

    private void startEskalering() {
        val e = EskaleringsvarselData.builder()
                .aktorId(AKTOR_ID.get())
                .opprettetAv(SAKSBEHANDLER)
                .opprettetBegrunnelse(BEGRUNNELSE)
                .tilhorendeDialogId(0)
                .build();

        eskaleringsvarselRepository.create(e);
    }

}
