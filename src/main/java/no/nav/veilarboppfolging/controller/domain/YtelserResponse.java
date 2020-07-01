package no.nav.veilarboppfolging.controller.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Vedtak;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Ytelseskontrakt;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingskontraktData;

import java.util.List;

@SuppressWarnings("unused")
public class YtelserResponse {
    private List<Vedtak> vedtaksliste;
    private List<Ytelseskontrakt> ytelser;
    @JsonProperty("oppfolgingskontrakter")
    private List<OppfolgingskontraktData> oppfoelgingskontrakter;

    public YtelserResponse withVedtaksliste(List<Vedtak> vedtaksliste) {
        this.vedtaksliste = vedtaksliste;
        return this;
    }

    public YtelserResponse withYtelser(List<Ytelseskontrakt> ytelser) {
        this.ytelser = ytelser;
        return this;
    }

    public YtelserResponse withOppfoelgingskontrakter(List<OppfolgingskontraktData> oppfoelgingskontrakter) {
        this.oppfoelgingskontrakter = oppfoelgingskontrakter;
        return this;
    }

    public List<Vedtak> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<Ytelseskontrakt> getYtelser() {
        return ytelser;
    }

    public List<OppfolgingskontraktData> getOppfoelgingskontrakter() {
        return oppfoelgingskontrakter;
    }
}
