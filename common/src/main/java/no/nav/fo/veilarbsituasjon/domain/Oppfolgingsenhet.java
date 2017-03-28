package no.nav.fo.veilarbsituasjon.domain;

public class Oppfolgingsenhet {
    private String navn;
    private String enhetId;

    public Oppfolgingsenhet withNavn(String navn) {
        this.navn = navn;
        return this;
    }

    public Oppfolgingsenhet withEnhetId(String enhetId) {
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
