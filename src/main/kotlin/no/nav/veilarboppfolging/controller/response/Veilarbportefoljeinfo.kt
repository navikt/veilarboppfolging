package no.nav.veilarboppfolging.controller.response

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NavIdent
import java.time.ZonedDateTime


data class Veilarbportefoljeinfo(
    val aktorId: AktorId,
    val veilederId: NavIdent?,
    val erUnderOppfolging: Boolean,
    val nyForVeileder: Boolean,
    val erManuell: Boolean,
    val startDato: ZonedDateTime? = null,
    val tilordnetTidspunkt: ZonedDateTime? = null
)
