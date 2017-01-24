package no.nav.fo.veilarbsituasjon.rest.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
public class YtelserResponse {
    private List<Vedtak> vedtaksliste;
    private List<Ytelseskontrakt> ytelser;
    @JsonProperty("oppfolgingskontrakter")
    private List<Oppfoelgingskontrakt> oppfoelgingskontrakter;

    public YtelserResponse withVedtaksliste(List<Vedtak> vedtaksliste) {
        this.vedtaksliste = vedtaksliste;
        return this;
    }

    public YtelserResponse withYtelser(List<Ytelseskontrakt> ytelser) {
        this.ytelser = ytelser;
        return this;
    }

    public YtelserResponse withInnsatsgruppe(List<Oppfoelgingskontrakt> oppfoelgingskontrakter) {
        this.oppfoelgingskontrakter = oppfoelgingskontrakter;
        return this;
    }

    public List<Vedtak> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<Ytelseskontrakt> getYtelser() {
        return ytelser;
    }

    public List<Oppfoelgingskontrakt> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
