package no.nav.veilarboppfolging.controller.v2.request;

import lombok.Data;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

@Data
public class AvsluttOppfolgingV2Request {
    NavIdent veilederId;
    String begrunnelse;
    Fnr fnr;
}
