package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


class OppfolgingsenhetEndringServiceTest extends DatabaseTest {
    private final static String AKTOERID = "123";
    private final static String NYTT_NAV_KONTOR = "1111";

    private OppfolgingsenhetHistorikkRepository repo = new OppfolgingsenhetHistorikkRepository(new JdbcTemplate(setupInMemoryDatabase()));
    private OppfolgingsenhetEndringService service = new OppfolgingsenhetEndringService(repo);

    @Test
    void skal_legge_til_ny_enhet_i_historikk_gitt_eksisterende_historikk() {
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring("2222");

        List<OppfolgingsenhetEndringData> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOERID);

        assertThat(historikk.size(), is(2));
        assertThat(historikk.get(0).getEnhet(), equalTo("2222"));
        assertThat(historikk.get(1).getEnhet(), equalTo("1111"));
    }

    @Test
    void skal_legge_til_ny_enhet_i_historikk_gitt_tom_historikk() {
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);
        List<OppfolgingsenhetEndringData> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOERID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    void skal_ikke_legge_til_ny_enhet_i_historikk_hvis_samme_enhet_allerede_er_nyeste_historikk() {
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringData> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOERID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    void skal_legge_til_ny_enhet_med_samme_enhet_midt_i_historikken() {
        gitt_eksisterende_historikk("1234");
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        gitt_eksisterende_historikk("4321");

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringData> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOERID);

        assertThat(historikk.size(), is(4));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }


    private void behandle_ny_enhets_endring(String navKontor) {
        VeilarbArenaOppfolging arenaEndring = new VeilarbArenaOppfolging()
                .setAktoerid(AKTOERID)
                .setNav_kontor(navKontor);

        service.behandleBrukerEndring(arenaEndring);
    }

    private void gitt_eksisterende_historikk(String navKontor) {
        repo.insertOppfolgingsenhetEndringForAktorId(AKTOERID, navKontor);
    }
}