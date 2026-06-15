package no.nav.veilarboppfolging.controller.v2.request;


import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

public class AvsluttOppfolgingV2Request {
    NavIdent veilederId;
    String begrunnelse;
    Fnr fnr;
}
