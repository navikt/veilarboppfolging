package no.nav.fo.veilarboppfolging.vilkar;

import org.junit.Test;

import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VilkarServiceTest {

    private VilkarService vilkarService = new VilkarService();

    @Test
    public void getVilkar_kjenteSprak_riktigeVilkar() {
        assertThat(vilkarService.getVilkar(null, "nb"), equalTo("Vilkår nb"));
        assertThat(vilkarService.getVilkar(null, "nn"), equalTo("Vilkår nn"));
        assertThat(vilkarService.getVilkar(UNDER_OPPFOLGING, "nb"), equalTo("Vilkår nb"));
        assertThat(vilkarService.getVilkar(UNDER_OPPFOLGING, "nn"), equalTo("Vilkår nn"));
        assertThat(vilkarService.getVilkar(PRIVAT, "nb"), equalTo("Private vilkår nb"));
        assertThat(vilkarService.getVilkar(PRIVAT, "nn"), equalTo("Private vilkår nn"));
    }

    @Test
    public void getVilkar_ukjentSprak_brukBokmal() {
        assertThat(vilkarService.getVilkar(null, null), equalTo("Vilkår nb"));
    }

}
