package no.nav.veilarboppfolging.kandidatForUtmelding.dto

import no.nav.veilarboppfolging.kandidatForUtmelding.ForlengelseHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseUtfortAvType

data class KandidatForUtmeldingHendelseDto(
    val utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    val utfortAv: String?,
    val hendelseTidspunkt: String,
    val type: KandidatForUtmeldingHendelseTypeDto,
    val forlengetTil: String?,
)

enum class KandidatForUtmeldingHendelseTypeDto {
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
    FORLENGELSE_OPPRETTET,
    FORLENGELSE_ENDRET,
    FORLENGELSE_UTLOPT
}

fun KandidatForUtmeldingHendelse.toKandidatForUtmeldingHendelseDto() = KandidatForUtmeldingHendelseDto(
    utfortAvType = utfortAvType,
    utfortAv = utfortAv,
    hendelseTidspunkt = hendelseTidspunkt.toString(),
    type = KandidatForUtmeldingHendelseTypeDto.valueOf(type.toString()),
    forlengetTil = (this as? ForlengelseHendelse)?.hentForlengetTil()?.toString()
)
