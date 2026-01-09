package no.nav.veilarboppfolging.controller.graphql.brukerStatus

data class BrukerStatusDto(
    val erKontorSperret: Boolean? = null,
    val manuell: Boolean? = null,
    val arena: BrukerStatusArenaDto? = null,
    val krr: BrukerStatusKrrDto? = null,
    val kontorSperre: KontorSperre? = null,
    val tilordnetVeileder: VeilederTilordningDto? = null,
)
