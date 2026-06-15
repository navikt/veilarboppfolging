package no.nav.veilarboppfolging.repository.entity

import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import java.time.ZonedDateTime

data class ManuellStatusEntity(
    val id: Long,
    val aktorId: String,
    val manuell: Boolean,
    val dato: ZonedDateTime,
    val begrunnelse: String?,
    val opprettetAv: KodeverkBruker?,
    val opprettetAvBrukerId: String?,
)
