package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class OppfolgingsenhetEndringServiceTest {
    private final static AktorId AKTOR_ID = AktorId.of("123");
    private final static String NYTT_NAV_KONTOR = "1111";

    private OppfolgingsenhetHistorikkRepository repo = new OppfolgingsenhetHistorikkRepository(LocalH2Database.getDb());
    private OppfolgingsenhetEndringService service = new OppfolgingsenhetEndringService(repo);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skal_legge_til_ny_enhet_i_historikk_gitt_eksisterende_historikk() {
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring("2222");

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(2));
        assertThat(historikk.get(0).getEnhet(), equalTo("2222"));
        assertThat(historikk.get(1).getEnhet(), equalTo("1111"));
    }

    @Test
    public void skal_legge_til_ny_enhet_i_historikk_gitt_tom_historikk() {
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);
        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    public void skal_ikke_legge_til_ny_enhet_i_historikk_hvis_samme_enhet_allerede_er_nyeste_historikk() {
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    public void skal_legge_til_ny_enhet_med_samme_enhet_midt_i_historikken() {
        gitt_eksisterende_historikk("1234");
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        gitt_eksisterende_historikk("4321");

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(4));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }


    private void behandle_ny_enhets_endring(String navKontor) {
        EndringPaaOppfoelgingsBrukerV1 arenaEndring = new EndringPaaOppfoelgingsBrukerV1()
                .setAktoerid(AKTOR_ID.get())
                .setNav_kontor(navKontor)
                .setFormidlingsgruppekode("ARBS");

        service.behandleBrukerEndring(arenaEndring);
    }

    private void gitt_eksisterende_historikk(String navKontor) {
        repo.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, navKontor);
    }
}
