package no.nav.veilarboppfolging.controller.v2.response;


import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto;

import java.util.List;

public class YtelserV2Response {
    List<VedtakDto> vedtaksliste;
    List<YtelseskontraktDto> ytelser;
}
