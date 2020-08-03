package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


class OppfolgingsenhetEndringServiceTest {
    private final static String AKTOERID = "123";
    private final static String NYTT_NAV_KONTOR = "1111";

    private OppfolgingsenhetHistorikkRepository repo = new OppfolgingsenhetHistorikkRepository(LocalH2Database.getDb());
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
        VeilarbArenaOppfolgingEndret arenaEndring = new VeilarbArenaOppfolgingEndret()
                .setAktoerid(AKTOERID)
                .setNav_kontor(navKontor);

        service.behandleBrukerEndring(arenaEndring);
    }

    private void gitt_eksisterende_historikk(String navKontor) {
        repo.insertOppfolgingsenhetEndringForAktorId(AKTOERID, navKontor);
    }
}
