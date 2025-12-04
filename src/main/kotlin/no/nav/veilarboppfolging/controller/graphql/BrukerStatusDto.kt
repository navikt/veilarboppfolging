package no.nav.veilarboppfolging.controller.graphql

data class BrukerStatusDto(
    val erKontorSperret: Boolean? = null,
    val manuell: Boolean? = null,
    val arena: BrukerStatusArenaDto? = null,
    val krr: BrukerStatusKrrDto? = null
)