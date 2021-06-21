package no.nav.veilarboppfolging.controller.request;

public enum Innsatsgruppe {
    STANDARD_INNSATS("IKVAL"),
    SITUASJONSBESTEMT_INNSATS("BFORM"),
    BEHOV_FOR_ARBEIDSEVNEVURDERING("BKART");

    private String innsatsgruppeKode;

    Innsatsgruppe(String innsatsgruppeKode) {
        this.innsatsgruppeKode = innsatsgruppeKode;
    }

    public String getKode() {
        return innsatsgruppeKode;
    }
}
