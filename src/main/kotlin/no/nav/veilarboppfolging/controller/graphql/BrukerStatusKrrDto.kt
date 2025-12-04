package no.nav.veilarboppfolging.controller.graphql

data class BrukerStatusKrrDto(
    val kanVarsles: Boolean? = null,
    val registrertIKrr: Boolean? = null,
    val reservertIKrr: Boolean? = null
)