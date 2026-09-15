package no.nav.veilarboppfolging.controller.graphql.veilederTilgang

import no.nav.veilarboppfolging.controller.graphql.TilgangResultat

data class VeilederTilgangDto(
    @Deprecated("Erstattet av harVeilederLeseTilgangTilBruker som er mer beskrivende")
    val harTilgang: Boolean?,
    val harVeilederLeseTilgangTilBruker: Boolean?,
    val harVeilederLeseTilgangTilKontorsperretBruker: Boolean?,
    val harVeilederLeseTilgangTilBrukersEnhet: Boolean?,
    val harVeilederTilgangFlytteBrukerTilEgetKontor: Boolean?,
    val tilgang: TilgangResultat?,
    val harAktiveTiltaksdeltakelserVedFlyttingTilEgetKontor: Boolean?,
    val harVeilederTilgangStarteOppfolging: Boolean?,
)
