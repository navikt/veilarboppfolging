package no.nav.fo.veilarbsituasjon.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingskontraktData;

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
