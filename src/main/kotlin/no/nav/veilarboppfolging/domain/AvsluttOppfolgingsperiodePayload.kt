package no.nav.veilarboppfolging.domain


data class AvsluttOppfolgingsperiodePayload(
    val aktorId: String,
    val begrunnelse: String,
    val oppfolgingsperiodeUuid: String,
)
