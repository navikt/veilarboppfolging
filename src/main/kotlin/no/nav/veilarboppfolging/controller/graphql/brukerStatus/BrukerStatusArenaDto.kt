package no.nav.veilarboppfolging.controller.graphql.brukerStatus

data class BrukerStatusArenaDto(
    val inaktivIArena: Boolean?,
    val kanReaktiveres: Boolean?,
    val inaktiveringsdato: String?,
    val kvalifiseringsgruppe: String?,
    val formidlingsgruppe: String?,
)
