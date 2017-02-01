package no.nav.fo.veilarbsituasjon.rest.domain;

public class VeilederTilordning {

    private String identVeileder;
    private String fodselsnummerBruker;

    public String getIdentVeileder() {
        return identVeileder;
    }

    public VeilederTilordning setIdentVeileder(String identVeileder) {
        this.identVeileder = identVeileder;
        return this;
    }

    public String getFodselsnummerBruker() {
        return fodselsnummerBruker;
    }

    public VeilederTilordning setFodselsnummerBruker(String fodselsnummerBruker) {
        this.fodselsnummerBruker = fodselsnummerBruker;
        return this;
    }
}
