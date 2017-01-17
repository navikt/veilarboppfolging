package no.nav.fo.veilarbsituasjon.rest.domain;


import java.util.List;

public class YtelseskontraktResponse {
    private final List<Vedtak> vedtakList;
    private final List<Ytelseskontrakt> ytelser;

    YtelseskontraktResponse(List<Vedtak> vedtakList, List<Ytelseskontrakt> ytelser) {

        this.vedtakList = vedtakList;
        this.ytelser = ytelser;
    }

    public List<Vedtak> getVedtakList() {
        return vedtakList;
    }

    public List<Ytelseskontrakt> getYtelser() {
        return ytelser;
    }
}
