package no.nav.veilarboppfolging.repository.entity

import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import java.time.ZonedDateTime

data class KvpPeriodeEntity(
    val kvpId: Long?,
    val serial: Long?,
    val aktorId: String,
    val enhet: String,
    val opprettetAv: String?,
    val opprettetDato: ZonedDateTime,
    val opprettetBegrunnelse: String? = null,
    val opprettetKodeverkbruker: KodeverkBruker? = null,
    val avsluttetAv: String? = null,
    val avsluttetDato: ZonedDateTime? = null,
    val avsluttetBegrunnelse: String? = null,
    val avsluttetKodeverkbruker: KodeverkBruker? = null,
)