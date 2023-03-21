package no.nav.veilarboppfolging.controller.response;


import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto;

import java.util.List;

public class YtelserResponse {
    private List<VedtakDto> vedtaksliste;
    private List<YtelseskontraktDto> ytelser;

    public YtelserResponse withVedtaksliste(List<VedtakDto> vedtaksliste) {
        this.vedtaksliste = vedtaksliste;
        return this;
    }

    public YtelserResponse withYtelser(List<YtelseskontraktDto> ytelser) {
        this.ytelser = ytelser;
        return this;
    }

    public List<VedtakDto> getVedtaksliste() {
        return vedtaksliste;
    }

    public List<YtelseskontraktDto> getYtelser() {
        return ytelser;
    }
}
