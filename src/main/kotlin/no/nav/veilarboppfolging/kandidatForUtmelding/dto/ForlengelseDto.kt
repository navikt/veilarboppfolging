package no.nav.veilarboppfolging.kandidatForUtmelding.dto

import no.nav.veilarboppfolging.kandidatForUtmelding.ForlengelseHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseUtfortAvType

data class ForlengelseDto(
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val hendelseTidspunkt: String,
    val forlengetTil: String,
)

fun ForlengelseHendelse.toDto(): ForlengelseDto {
    return ForlengelseDto(
        utfortAvType = utfortAvType,
        utfortAv = utfortAv,
        hendelseTidspunkt = hendelseTidspunkt.toString(),
        forlengetTil = hentForlengetTil()?.toString() ?: throw IllegalStateException("ForlengelseHendelse må ha en forlengetTil"),
    )
}
