package no.nav.veilarboppfolging.controller.response;



import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;

import java.time.ZonedDateTime;


public class Veilarbportefoljeinfo {
    private AktorId aktorId;

    private NavIdent veilederId;
    private boolean erUnderOppfolging;
    private boolean nyForVeileder;
    private boolean erManuell;
    private ZonedDateTime startDato;
    private ZonedDateTime tilordnetTidspunkt;
}
