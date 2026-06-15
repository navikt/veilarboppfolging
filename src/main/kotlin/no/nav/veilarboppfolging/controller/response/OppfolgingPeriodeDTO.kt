package no.nav.veilarboppfolging.controller.response

import java.time.ZonedDateTime
import java.util.*

data class OppfolgingPeriodeDTO(
    val uuid: UUID,
    val aktorId: String,
    val veileder: String?,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?,
    val begrunnelse: String?,
    val kvpPerioder: List<KvpPeriodeDTO>
)
