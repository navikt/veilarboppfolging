package no.nav.fo.veilarbsituasjon.rest.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OppfoelgingskontraktResponse {

    @JsonProperty("oppfolgingskontrakter")
    private final List<Oppfoelgingskontrakt> oppfoelgingskontrakter;

    OppfoelgingskontraktResponse(List<Oppfoelgingskontrakt> oppfoelgingskontraktListe) {

        this.oppfoelgingskontrakter = oppfoelgingskontraktListe;
    }

    public List<Oppfoelgingskontrakt> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
