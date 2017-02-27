package no.nav.fo.veilarbsituasjon.rest.domain;

import java.util.List;

@SuppressWarnings("unused")
public class OppfoelgingskontraktData {
    private List<String> innsatsgrupper;
    private String status;

    public List<String> getInnsatsgrupper() {
        return innsatsgrupper;
    }

    public String getStatus() {
        return status;
    }

    public OppfoelgingskontraktData withStatus(String status) {
        this.status = status;
        return this;
    }

    public OppfoelgingskontraktData withInnsatsgruppe(List<String> insatsgrupper) {
        this.innsatsgrupper = insatsgrupper;
        return this;
    }
}
