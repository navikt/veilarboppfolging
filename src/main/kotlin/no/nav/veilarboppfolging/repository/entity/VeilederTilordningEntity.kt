package no.nav.veilarboppfolging.repository.entity

import java.time.ZonedDateTime

data class VeilederTilordningEntity(
    val aktorId: String,
    val veilederId: String,
    val oppfolging: Boolean,
    val nyForVeileder: Boolean,
    val sistTilordnet: ZonedDateTime?,
    val sistOppdatert: ZonedDateTime?,
)
