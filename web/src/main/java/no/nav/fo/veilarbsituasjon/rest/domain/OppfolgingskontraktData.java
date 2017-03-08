package no.nav.fo.veilarbsituasjon.rest.domain;

import java.util.List;

@SuppressWarnings("unused")
public class OppfolgingskontraktData {
    private List<String> innsatsgrupper;
    private String status;

    public List<String> getInnsatsgrupper() {
        return innsatsgrupper;
    }

    public String getStatus() {
        return status;
    }

    public OppfolgingskontraktData withStatus(String status) {
        this.status = status;
        return this;
    }

    public OppfolgingskontraktData withInnsatsgruppe(List<String> insatsgrupper) {
        this.innsatsgrupper = insatsgrupper;
        return this;
    }
}
