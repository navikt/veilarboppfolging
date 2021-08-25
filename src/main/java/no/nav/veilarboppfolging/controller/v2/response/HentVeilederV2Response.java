package no.nav.veilarboppfolging.controller.v2.response;

import lombok.Value;
import no.nav.common.types.identer.NavIdent;

@Value
public class HentVeilederV2Response {
    NavIdent veilederIdent;
}
