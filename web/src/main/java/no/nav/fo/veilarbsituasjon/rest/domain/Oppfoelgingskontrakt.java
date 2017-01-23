package no.nav.fo.veilarbsituasjon.rest.domain;

import java.util.List;

@SuppressWarnings("unused")
class Oppfoelgingskontrakt {
    private List<String> innsatsgrupper;
    private String status;

    public List<String> getInnsatsgrupper() {
        return innsatsgrupper;
    }

    public String getStatus() {
        return status;
    }

    Oppfoelgingskontrakt withStatus(String status) {
        this.status = status;
        return this;
    }

    Oppfoelgingskontrakt withInnsatsgruppe(List<String> insatsgrupper) {
        this.innsatsgrupper = insatsgrupper;
        return this;
    }
}
