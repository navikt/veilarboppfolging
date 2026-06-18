package no.nav.veilarboppfolging.repository.entity

import java.time.ZonedDateTime


data class VeilederTilordningHistorikkEntity(
    val veileder: String,
    val sistTilordnet: ZonedDateTime,
    val tilordnetAvVeileder: String?,
)
