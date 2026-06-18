package no.nav.veilarboppfolging.controller.response

import java.time.ZonedDateTime
import kotlin.Comparable
import kotlin.Int
import kotlin.String

data class KvpDTO(
    val kvpId: Long,
    val serial: Long,
    val aktorId: String,
    val enhet: String,
    val opprettetAv: String?,
    val opprettetDato: ZonedDateTime,
    val opprettetBegrunnelse: String?,
    val avsluttetAv: String?,
    val avsluttetDato: ZonedDateTime?,
    val avsluttetBegrunnelse: String?,

): Comparable<KvpDTO?> {
    override fun compareTo(other: KvpDTO?): Int {
        return (serial - (other?.serial ?: - 1)).toInt()
    }
}