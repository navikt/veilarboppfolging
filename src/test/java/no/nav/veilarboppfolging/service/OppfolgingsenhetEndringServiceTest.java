package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OppfolgingsenhetEndringServiceTest {

    private final static Fnr FNR = Fnr.of("12356");
    private final static AktorId AKTOR_ID = AktorId.of("123");
    private final static String NYTT_NAV_KONTOR = "1111";

    private AuthService authService = mock(AuthService.class);

    private OppfolgingsenhetHistorikkRepository repo = new OppfolgingsenhetHistorikkRepository(LocalDatabaseSingleton.INSTANCE.getJdbcTemplate());
    private OppfolgingsenhetEndringService service = new OppfolgingsenhetEndringService(repo, authService);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skal_legge_til_ny_enhet_i_historikk_gitt_eksisterende_historikk() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring("2222");

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(2));
        assertThat(historikk.get(0).getEnhet(), equalTo("2222"));
        assertThat(historikk.get(1).getEnhet(), equalTo("1111"));
    }

    @Test
    public void skal_legge_til_ny_enhet_i_historikk_gitt_tom_historikk() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);
        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    public void skal_ikke_legge_til_ny_enhet_i_historikk_hvis_samme_enhet_allerede_er_nyeste_historikk() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(1));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }

    @Test
    public void skal_legge_til_ny_enhet_med_samme_enhet_midt_i_historikken() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        gitt_eksisterende_historikk("1234");
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR);
        gitt_eksisterende_historikk("4321");

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR);

        List<OppfolgingsenhetEndringEntity> historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID);

        assertThat(historikk.size(), is(4));
        assertThat(historikk.get(0).getEnhet(), equalTo(NYTT_NAV_KONTOR));
    }


    private void behandle_ny_enhets_endring(String navKontor) {
        EndringPaaOppfoelgingsBrukerV2 arenaEndring = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(FNR.get())
                .oppfolgingsenhet(navKontor)
                .formidlingsgruppe(Formidlingsgruppe.ARBS)
                .build();

        service.behandleBrukerEndring(arenaEndring);
    }

    private void gitt_eksisterende_historikk(String navKontor) {
        repo.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, navKontor);
    }
}
