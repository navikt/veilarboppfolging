package no.nav.veilarboppfolging.controller.v2.request

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent


data class AvsluttOppfolgingV2Request(
    val veilederId: NavIdent,
    val begrunnelse: String,
    val fnr: Fnr,
)
