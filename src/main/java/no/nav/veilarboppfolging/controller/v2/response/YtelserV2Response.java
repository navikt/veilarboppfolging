package no.nav.veilarboppfolging.controller.v2.response;

import lombok.Value;
import no.nav.veilarboppfolging.client.ytelseskontrakt.VedtakDto;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktDto;

import java.util.List;

@Value
public class YtelserV2Response {
    List<VedtakDto> vedtaksliste;
    List<YtelseskontraktDto> ytelser;
}
