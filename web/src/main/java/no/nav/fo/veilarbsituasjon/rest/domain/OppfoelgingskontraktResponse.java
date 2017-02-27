package no.nav.fo.veilarbsituasjon.rest.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OppfoelgingskontraktResponse {

    @JsonProperty("oppfolgingskontrakter")
    private final List<OppfoelgingskontraktData> oppfoelgingskontrakter;

    public OppfoelgingskontraktResponse(List<OppfoelgingskontraktData> oppfoelgingskontraktListe) {

        this.oppfoelgingskontrakter = oppfoelgingskontraktListe;
    }

    public List<OppfoelgingskontraktData> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
