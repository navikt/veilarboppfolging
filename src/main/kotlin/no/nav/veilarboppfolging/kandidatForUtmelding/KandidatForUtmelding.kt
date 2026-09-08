package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmelding.Companion.KARENSTID_DAGER
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

sealed class KandidatForUtmelding(
    val sisteHendelse: KandidatForUtmeldingHendelse,
    val oppfolgingsperiodeId: UUID = sisteHendelse.oppfolgingsperiodeUuid
) {
    companion object {
        fun fromHendelse(hendelse: KandidatForUtmeldingHendelse): KandidatForUtmelding {
            val avsluttesAutomatiskDato = beregnAvsluttesAutomatiskDato(hendelse)
            val forlengetTil = when (hendelse) {
                is ForlengelseOpprettetEllerEndretHendelse -> hendelse.forlengetTil
                else -> null
            }
            return when {
                forlengetTil != null ->  ForlengetKandidat(hendelse as ForlengelseOpprettetEllerEndretHendelse, forlengetTil)
                avsluttesAutomatiskDato != null -> AktivKandidatForUtmelding(hendelse, avsluttesAutomatiskDato)
                else -> throw IllegalArgumentException("Hendelse må ha enten forlengetTil eller avsluttesAutomatiskDato")
            }
        }

        /**
         * Datoen kandidaten skal avsluttes automatisk fra oppfølging, gitt denne hendelsen.
         * Kandidaten skal avsluttes automatisk [KARENSTID_DAGER] dager etter at de først ble kandidat for utmelding,
         * eller [KARENSTID_DAGER] dager etter at en eventuell forlengelse utløp.
         * Mens en forlengelse er aktiv (FORLENGELSE_OPPRETTET/FORLENGELSE_ENDRET) er datoen pauset (null).
         */
        private fun beregnAvsluttesAutomatiskDato(hendelse: KandidatForUtmeldingHendelse): LocalDateTime? {
            val hendelseTid = LocalDateTime.ofInstant(hendelse.hendelseTidspunkt, ZoneOffset.UTC)
            return when (hendelse) {
                is ArbeidssøkerPeriodeAvsluttet -> hendelseTid.plusDays(KARENSTID_DAGER)
                is ForlengelseOpprettetEllerEndretHendelse ->  null
                is ForlengelseUtløptHendelse -> hendelseTid.plusDays(KARENSTID_DAGER)
                is OppfolgingAvsluttetHendelse -> when (hendelse.type) {
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK,
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT -> null
                }
            }
        }

        const val KARENSTID_DAGER = 28L
    }
}

fun beregnAvsluttesAutomatiskDato(hendelseTidspunkt: Instant): ZonedDateTime {
    val hendelseTid = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
    return hendelseTid.plusDays(KARENSTID_DAGER).atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Europe/Oslo"))
}

class ForlengetKandidat(
    val forlengelseHendelse: ForlengelseOpprettetEllerEndretHendelse,
    val forlengetTil: LocalDate): KandidatForUtmelding(forlengelseHendelse)

class AktivKandidatForUtmelding(sisteHendelse: KandidatForUtmeldingHendelse,
    val avsluttesAutomatiskDato: LocalDateTime
): KandidatForUtmelding(sisteHendelse)
