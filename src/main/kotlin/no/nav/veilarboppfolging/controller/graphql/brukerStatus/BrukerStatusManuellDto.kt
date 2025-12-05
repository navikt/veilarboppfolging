package no.nav.veilarboppfolging.controller.graphql.brukerStatus

data class BrukerStatusManuellDto(
    val erManuell: Boolean,
    val tidspunkt: String,
    val begrunnelse: String,
    val endretAvType: String,
    val endretAvIdent: String
)