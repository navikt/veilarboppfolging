package no.nav.veilarboppfolging.controller.graphql.oppfolging

data class OppfolgingsEnhetQueryDto(
    val enhet: EnhetDto?, // Nullable because graphql
)

data class EnhetDto(
    val id: String,
    val navn: String,
    val kilde: KildeDto
)

enum class KildeDto {
    ARENA,
    NORG,
    AOKONTOR
}
