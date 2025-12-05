package no.nav.veilarboppfolging.controller.graphql.veilederTilgang

import no.nav.veilarboppfolging.controller.graphql.TilgangResultat

data class VeilederTilgangDto(
    val harTilgang: Boolean?,
    val harSkriveTilgang: Boolean?,
    val tilgang: TilgangResultat?
)