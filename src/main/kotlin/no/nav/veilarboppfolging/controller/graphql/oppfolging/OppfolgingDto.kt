package no.nav.veilarboppfolging.controller.graphql.oppfolging

import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto

data class OppfolgingDto(
    val erUnderOppfolging: Boolean?,
    val kanStarteOppfolging: KanStarteOppfolgingDto?,
)

data class OppfolgingsperiodeDto(
    val startTidspunkt: String,
    val sluttTidspunkt: String?,
    val id: String,
    val startetBegrunnelse: String?
)