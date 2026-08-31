package no.nav.veilarboppfolging.kandidatForUtmelding.dto

data class Forlengelse(
    val opprettetAv: String,
    val opprettetTidspunkt: String,
    val forlengelseType: ForlengelseType,
    val forlengetTil: String,
)

enum class ForlengelseType {
    FORLENGELSE_OPPRETTET,
    FORLENGELSE_ENDRET,
}
