package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.LocalDate
import java.time.LocalDateTime

sealed class KandidatForUtmelding(
    val sisteHendelse: KandidatForUtmeldingHendelse
) {
    companion object {
        fun fromHendelse(hendelse: KandidatForUtmeldingHendelse): KandidatForUtmelding {
            val avsluttesAutomatiskDato = hendelse.beregnAvsluttesAutomatiskDato()
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
    }
}

class ForlengetKandidat(
    sisteHendelse: KandidatForUtmeldingHendelse,
    val forlengetTil: LocalDate): KandidatForUtmelding(sisteHendelse)

class AktivKandidatForUtmelding(sisteHendelse: KandidatForUtmeldingHendelse,
    val avsluttesAutomatiskDato: LocalDateTime
): KandidatForUtmelding(sisteHendelse)