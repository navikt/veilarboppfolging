package no.nav.veilarboppfolging.controller.graphql

data class BrukerStatusArenaDto(
    val inaktivIArena: Boolean?,
    val kanReaktiveres: Boolean?,
    val inaktiveringsdato: String?,
    val kvalifiseringsgruppe: String?,
)