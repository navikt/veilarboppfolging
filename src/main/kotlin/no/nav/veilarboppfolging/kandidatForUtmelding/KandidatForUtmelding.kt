package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelse.Companion.KARENSTID_DAGER
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed class KandidatForUtmelding(
    val sisteHendelse: KandidatForUtmeldingHendelse
) {
    companion object {
        fun fromHendelse(hendelse: KandidatForUtmeldingHendelse): KandidatForUtmelding {
            val avsluttesAutomatiskDato = beregnAvsluttesAutomatiskDato(hendelse)
            val forlengetTil = when (hendelse) {
                is ForlengelseHendelse -> hendelse.hentForlengetTil()
                else -> null
            }
            return when {
                forlengetTil != null ->  ForlengetKandidat(hendelse, forlengetTil)
                avsluttesAutomatiskDato != null -> AktivKandidatForUtmelding(hendelse, avsluttesAutomatiskDato)
                else -> throw IllegalArgumentException("Hendelse må ha enten forlengetTil eller avsluttesAutomatiskDato")
            }
        }

        private fun beregnAvsluttesAutomatiskDato(hendelse: KandidatForUtmeldingHendelse): LocalDateTime? {
            val hendelseTid = LocalDateTime.ofInstant(hendelse.hendelseTidspunkt, ZoneOffset.UTC)
            return when (hendelse) {
                is ArbeidssøkerPeriodeAvsluttet -> hendelseTid.plusDays(KARENSTID_DAGER)
                is ForlengelseHendelse -> when (hendelse.type) {
                    ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                    ForlengelseHendelseType.FORLENGELSE_ENDRET -> null
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> hendelseTid.plusDays(KARENSTID_DAGER)
                }
                is OppfolgingAvsluttetHendelse -> when (hendelse.type) {
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK,
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT -> null
                }
            }
        }
    }
}

class ForlengetKandidat(
    sisteHendelse: KandidatForUtmeldingHendelse,
    val forlengetTil: LocalDate): KandidatForUtmelding(sisteHendelse)

class AktivKandidatForUtmelding(sisteHendelse: KandidatForUtmeldingHendelse,
    val avsluttesAutomatiskDato: LocalDateTime
): KandidatForUtmelding(sisteHendelse)