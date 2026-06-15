package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class KvpPeriode(
    val event: KvpPeriodeEventType,
    val aktorId: String,
    val enhetId: String,
    val startet: KvpStartet,
    val avsluttet: KvpAvsluttet?
) {

    fun tilAvsluttetKvpPeriode(avsluttetAv: String, avsluttetTidspunkt: ZonedDateTime, begrunnelse: String): KvpPeriode {
        return this.copy(
            avsluttet = KvpAvsluttet(avsluttetAv, avsluttetTidspunkt, begrunnelse),
            event = KvpPeriodeEventType.AVSLUTTET
        )
    }

    companion object {
        fun start(
            aktorId: AktorId,
            enhetId: String,
            opprettetAv: String,
            startDato: ZonedDateTime,
            begrunnelse: String?
        ): KvpPeriode {
            val kvpStartet = KvpStartet(opprettetAv, startDato, begrunnelse)
            return KvpPeriode(KvpPeriodeEventType.STARTET, aktorId.get(), enhetId, kvpStartet, null)
        }
    }
}


