package no.nav.veilarboppfolging.kandidatForUtmelding.dto

import no.nav.veilarboppfolging.kandidatForUtmelding.ForlengelseHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseUtfortAvType

data class KandidatForUtmeldingHendelseDto(
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val hendelseTidspunkt: String,
    val type: KandidatForUtmeldingHendelseType,
    val forlengetTil: String?,
)

fun KandidatForUtmeldingHendelse.toKandidatForUtmeldingHendelseDto() = KandidatForUtmeldingHendelseDto(
    utfortAvType = utfortAvType,
    utfortAv = utfortAv,
    hendelseTidspunkt = hendelseTidspunkt.toString(),
    type = type,
    forlengetTil = (this as? ForlengelseHendelse)?.hentForlengetTil()?.toString()
)
