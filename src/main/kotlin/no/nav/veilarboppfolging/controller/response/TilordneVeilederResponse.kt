package no.nav.veilarboppfolging.controller.response

import no.nav.veilarboppfolging.controller.request.VeilederTilordning


data class TilordneVeilederResponse(
    val resultat: String,
    val feilendeTilordninger: List<VeilederTilordning> = emptyList()
)
