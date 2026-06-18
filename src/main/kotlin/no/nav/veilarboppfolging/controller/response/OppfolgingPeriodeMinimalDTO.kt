package no.nav.veilarboppfolging.controller.response

import java.time.ZonedDateTime
import java.util.*

class OppfolgingPeriodeMinimalDTO(
    val uuid: UUID,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?
)
