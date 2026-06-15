package no.nav.veilarboppfolging.controller.response

import java.time.ZonedDateTime

data class KvpPeriodeDTO(
    val opprettetDato: ZonedDateTime,
    val avsluttetDato: ZonedDateTime?,
)
