package no.nav.veilarboppfolging.client.tiltakshistorikk

enum class ArbeidsgiverAvtaleStatusDto {
    PAABEGYNT,
    MANGLER_GODKJENNING,
    KLAR_FOR_OPPSTART,
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    ANNULLERT,
}

fun ArbeidsgiverAvtaleStatusDto.erAktiv() = this in listOf(
    ArbeidsgiverAvtaleStatusDto.KLAR_FOR_OPPSTART,
    ArbeidsgiverAvtaleStatusDto.GJENNOMFORES,
)
