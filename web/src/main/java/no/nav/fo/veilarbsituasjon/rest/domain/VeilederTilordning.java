package no.nav.fo.veilarbsituasjon.rest.domain;

public class VeilederTilordning {

    private String identVeileder;
    private String fodselsnummerOppfolgingsbruker;

    public String getIdentVeileder() {
        return identVeileder;
    }

    public VeilederTilordning setIdentVeileder(String identVeileder) {
        this.identVeileder = identVeileder;
        return this;
    }

    public String getFodselsnummerOppfolgingsbruker() {
        return fodselsnummerOppfolgingsbruker;
    }

    public VeilederTilordning setFodselsnummerOppfolgingsbruker(String fodselsnummerOppfolgingsbruker) {
        this.fodselsnummerOppfolgingsbruker = fodselsnummerOppfolgingsbruker;
        return this;
    }
}
