package no.nav.fo.veilarbsituasjon.vilkar;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VilkarDataServiceTest {

    private VilkarService vilkarService = new VilkarService();

    @Before
    public void setup(){
        vilkarService.vilkarPath = VilkarDataServiceTest.class.getResource("/vilkar/").getPath();
    }

    @Test
    public void getVilkar_kjenteSprak_riktigeVilkar(){
        assertThat(vilkarService.getVilkar("nb"),equalTo("Vilkår nb"));
        assertThat(vilkarService.getVilkar("nn"),equalTo("Vilkår nn"));
    }

    @Test
    public void getVilkar_ukjentSprak_brukBokmal(){
        assertThat(vilkarService.getVilkar(null),equalTo("Vilkår nb"));
    }

    @Test
    public void ping_vellykket() {
        assertThat(vilkarService.ping().isVellykket(), is(true));
    }

    @Test
    public void ping_feilPath_feiler() {
        vilkarService.vilkarPath = "/feil/path";
        assertThat(vilkarService.ping().isVellykket(), is(false));
    }

}