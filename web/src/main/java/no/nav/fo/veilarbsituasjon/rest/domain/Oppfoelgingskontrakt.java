package no.nav.fo.veilarbsituasjon.rest.domain;

import java.util.List;

class Oppfoelgingskontrakt {
    private List<String> insatsgrupper;

    Oppfoelgingskontrakt withInnsatsgruppe(List<String> insatsgrupper) {
        this.insatsgrupper = insatsgrupper;
        return this;
    }

    public List<String> getInsatsgrupper() {
        return insatsgrupper;
    }
}
