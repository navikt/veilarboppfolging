package no.nav.veilarboppfolging.controller.admin.v1

data class AvsluttPayload(
    val aktorIds: List<String>,
    val begrunnelse: String,
)
