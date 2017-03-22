package no.nav.fo.veilarbsituasjon.rest.domain;

public class Veileder {

    private String veilederident;

    public String getVeilederident() {
        return veilederident;
    }

    public Veileder withIdent(String veilederident) {
        this.veilederident = veilederident;
        return this;
    }
}
