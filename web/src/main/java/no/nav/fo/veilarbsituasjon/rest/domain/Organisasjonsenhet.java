package no.nav.fo.veilarbsituasjon.rest.domain;

public class Organisasjonsenhet {
    private String navn;
    private String enhetId;

    public Organisasjonsenhet withNavn(String navn) {
        this.navn = navn;
        return this;
    }

    public Organisasjonsenhet withEnhetId(String enhetId) {
        this.enhetId = enhetId;
        return this;
    }

    public String getNavn() {
        return navn;
    }

    public String getEnhetId() {
        return enhetId;
    }
}
