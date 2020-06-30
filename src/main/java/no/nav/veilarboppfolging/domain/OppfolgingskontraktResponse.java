package no.nav.veilarboppfolging.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OppfolgingskontraktResponse {

    @JsonProperty("oppfolgingskontrakter")
    private final List<OppfolgingskontraktData> oppfoelgingskontrakter;

    public OppfolgingskontraktResponse(List<OppfolgingskontraktData> oppfoelgingskontraktListe) {

        this.oppfoelgingskontrakter = oppfoelgingskontraktListe;
    }

    public List<OppfolgingskontraktData> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
