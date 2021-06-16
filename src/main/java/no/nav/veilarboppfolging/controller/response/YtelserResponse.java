package no.nav.veilarboppfolging.controller.response;


import no.nav.veilarboppfolging.client.ytelseskontrakt.Vedtak;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Ytelseskontrakt;

import java.util.List;

public class YtelserResponse {
    private List<Vedtak> vedtaksliste;
    private List<Ytelseskontrakt> ytelser;

    public YtelserResponse withVedtaksliste(List<Vedtak> vedtaksliste) {
        this.vedtaksliste = vedtaksliste;
        return this;
    }

    public YtelserResponse withYtelser(List<Ytelseskontrakt> ytelser) {
        this.ytelser = ytelser;
        return this;
    }

    public List<Vedtak> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<Ytelseskontrakt> getYtelser() {
        return ytelser;
    }
}
