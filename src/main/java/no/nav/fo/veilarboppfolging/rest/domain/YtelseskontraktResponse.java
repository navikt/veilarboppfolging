package no.nav.fo.veilarboppfolging.rest.domain;


import java.util.List;

public class YtelseskontraktResponse {
    private final List<Vedtak> vedtaksliste;
    private final List<Ytelseskontrakt> ytelser;

    public YtelseskontraktResponse(List<Vedtak> vedtaksliste, List<Ytelseskontrakt> ytelser) {

        this.vedtaksliste = vedtaksliste;
        this.ytelser = ytelser;
    }

    public List<Vedtak> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<Ytelseskontrakt> getYtelser() {
        return ytelser;
    }
}
