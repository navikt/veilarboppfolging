package no.nav.veilarboppfolging.controller.graphql.veilederTilgang

import no.nav.veilarboppfolging.controller.graphql.TilgangResultat

data class VeilederTilgangDto(
    @Deprecated("Erstattet av harVeilederLeseTilgangTilBruker som er mer beskrivende")
    val harTilgang: Boolean?,
    val harVeilederLeseTilgangTilBruker: Boolean?,
    val harVeilederLeseTilgangTilKontorsperretBruker: Boolean?,
    val harVeilederLeseTilgangTilBrukersEnhet: Boolean?,
    val tilgang: TilgangResultat?
)

/*
                                                Bryker er på kvp    Bruker er ikke under kvp
   Veileder har tilgang til brukers enhet           true,true,true,true                   true
   Veileder har ikke tilgang til brukers enhet      false                   true

   Veileder tilgang modia_generell
   Veileder tilgang modia_oppfolging

 */