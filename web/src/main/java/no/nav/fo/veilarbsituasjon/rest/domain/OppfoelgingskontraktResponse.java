package no.nav.fo.veilarbsituasjon.rest.domain;


import java.util.List;

public class OppfoelgingskontraktResponse {
    private final List<Oppfoelgingskontrakt> oppfoelgingskontrakter;

    OppfoelgingskontraktResponse(List<Oppfoelgingskontrakt> oppfoelgingskontraktListe) {

        this.oppfoelgingskontrakter = oppfoelgingskontraktListe;
    }

    public List<Oppfoelgingskontrakt> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
