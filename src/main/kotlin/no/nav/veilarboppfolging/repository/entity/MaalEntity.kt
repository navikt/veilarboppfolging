package no.nav.veilarboppfolging.repository.entity

import java.time.ZonedDateTime


data class MaalEntity(
    val id: Long,
    val aktorId: String,
    val mal: String,
    val endretAv: String?,
    val dato: ZonedDateTime
) {
    val endretAvFormattert: String
        get() = if (erEndretAvBruker()) "BRUKER" else "VEILEDER"

    fun erEndretAvBruker(): Boolean {
        return endretAv != null && endretAv == aktorId
    }

    fun oppdaterMedNyId(nyId: Long): MaalEntity = this.copy(id = nyId)
}