package no.nav.veilarboppfolging.controller.v2.response;

import lombok.Value;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Vedtak;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Ytelseskontrakt;

import java.util.List;

@Value
public class YtelserV2Response {
    List<Vedtak> vedtaksliste;
    List<Ytelseskontrakt> ytelser;
}
