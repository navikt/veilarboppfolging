package no.nav.veilarboppfolging.kandidatForUtmelding.dto

import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ForlengelseOpprettetEllerEndretHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.KandidatForUtmeldingHendelseUtfortAvType

data class ForlengelseDto(
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val hendelseTidspunkt: String,
    val forlengetTil: String,
)

fun ForlengelseOpprettetEllerEndretHendelse.toDto(): ForlengelseDto {
    return ForlengelseDto(
        utfortAvType = utfortAvType,
        utfortAv = utfortAv,
        hendelseTidspunkt = hendelseTidspunkt.toString(),
        forlengetTil = this.forlengetTil.toString()
    )
}
