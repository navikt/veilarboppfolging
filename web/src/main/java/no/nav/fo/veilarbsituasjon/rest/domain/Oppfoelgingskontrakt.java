package no.nav.fo.veilarbsituasjon.rest.domain;

import java.util.List;

class Oppfoelgingskontrakt {
    private List<String> insatsgrupper;
    private String status;

    public List<String> getInsatsgrupper() {
        return insatsgrupper;
    }

    public String getStatus() {
        return status;
    }

    public Oppfoelgingskontrakt withStatus(String status) {
        this.status = status;
        return this;
    }

    Oppfoelgingskontrakt withInnsatsgruppe(List<String> insatsgrupper) {
        this.insatsgrupper = insatsgrupper;
        return this;
    }
}
