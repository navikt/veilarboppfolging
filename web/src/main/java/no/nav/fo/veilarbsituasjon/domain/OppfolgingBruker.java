package no.nav.fo.veilarbsituasjon.domain;


public class OppfolgingBruker {

    private String aktoerid;
    private String veileder;

    public String getAktoerid() {
        return aktoerid;
    }

    public OppfolgingBruker withAktoerid(String aktoerid) {
        this.aktoerid = aktoerid;
        return this;
    }

    public String getVeileder() {
        return veileder;
    }

    public OppfolgingBruker withVeileder(String veileder) {
        this.veileder = veileder;
        return this;
    }

    public String toString() {
        return "{aktoerid:"+aktoerid+",veileder:"+veileder+"}";
    }
}
