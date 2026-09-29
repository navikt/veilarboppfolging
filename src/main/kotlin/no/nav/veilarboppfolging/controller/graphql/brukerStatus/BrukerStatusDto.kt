package no.nav.veilarboppfolging.controller.graphql.brukerStatus

import no.nav.veilarboppfolging.client.isoppfolgingstilfelle.OppfolgingstilfelleStatus

data class BrukerStatusDto(
    val erKontorSperret: Boolean? = null,
    val manuell: Boolean? = null,
    val arena: BrukerStatusArenaDto? = null,
    val krr: BrukerStatusKrrDto? = null,
    val kontorSperre: KontorSperre? = null,
    val tilordnetVeileder: VeilederTilordningDto? = null,
    val harAktiveTiltaksdeltakelser: Boolean? = null,
    val sykmeldtStatus: OppfolgingstilfelleStatus? = null,
    val ident: String? = null
)
