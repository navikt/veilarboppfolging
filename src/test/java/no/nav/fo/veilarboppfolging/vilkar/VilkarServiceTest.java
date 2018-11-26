package no.nav.fo.veilarboppfolging.vilkar;

import org.junit.Before;
import org.junit.Test;

import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VilkarServiceTest {

    private VilkarService vilkarService = new VilkarService();

    @Before
    public void setup(){
        vilkarService.vilkarPath = VilkarServiceTest.class.getResource("/vilkar/").getPath();
    }

    @Test
    public void getVilkar_kjenteSprak_riktigeVilkar(){
        assertThat(vilkarService.getVilkar(null,"nb"),equalTo("Vilkår nb"));
        assertThat(vilkarService.getVilkar(null,"nn"),equalTo("Vilkår nn"));
        assertThat(vilkarService.getVilkar(UNDER_OPPFOLGING,"nb"),equalTo("Vilkår nb"));
        assertThat(vilkarService.getVilkar(UNDER_OPPFOLGING,"nn"),equalTo("Vilkår nn"));
        assertThat(vilkarService.getVilkar(PRIVAT,"nb"),equalTo("Private vilkår nb"));
        assertThat(vilkarService.getVilkar(PRIVAT,"nn"),equalTo("Private vilkår nn"));
    }

    @Test
    public void getVilkar_ukjentSprak_brukBokmal(){
        assertThat(vilkarService.getVilkar(null,null),equalTo("Vilkår nb"));
    }

    @Test
    public void ping_vellykket() {
        assertThat(vilkarService.ping().erVellykket(), is(true));
    }

    @Test
    public void ping_feilPath_feiler() {
        vilkarService.vilkarPath = "/feil/path";
        assertThat(vilkarService.ping().erVellykket(), is(false));
    }

}