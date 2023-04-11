package no.nav.veilarboppfolging.client.ytelseskontrakt;


import java.util.List;

public class YtelseskontraktResponse {
    private final List<VedtakDto> vedtaksliste;
    private final List<YtelseskontraktDto> ytelser;

    public YtelseskontraktResponse(List<VedtakDto> vedtaksliste, List<YtelseskontraktDto> ytelser) {
        this.vedtaksliste = vedtaksliste;
        this.ytelser = ytelser;
    }

    public List<VedtakDto> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<YtelseskontraktDto> getYtelser() {
        return ytelser;
    }
}
