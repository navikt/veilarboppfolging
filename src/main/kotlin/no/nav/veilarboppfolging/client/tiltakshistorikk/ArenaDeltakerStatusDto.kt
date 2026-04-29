package no.nav.veilarboppfolging.client.tiltakshistorikk

enum class ArenaDeltakerStatusDto {
    AKTUELL,
    AVSLAG,
    DELTAKELSE_AVBRUTT,
    FEILREGISTRERT,
    FULLFORT,
    GJENNOMFORES,
    GJENNOMFORING_AVBRUTT,
    GJENNOMFORING_AVLYST,
    IKKE_AKTUELL,
    IKKE_MOTT,
    INFORMASJONSMOTE,
    TAKKET_JA_TIL_TILBUD,
    TAKKET_NEI_TIL_TILBUD,
    TILBUD,
    VENTELISTE,
}

fun ArenaDeltakerStatusDto.erAktiv() = this in listOf(
    ArenaDeltakerStatusDto.AKTUELL,
    ArenaDeltakerStatusDto.GJENNOMFORES,
    ArenaDeltakerStatusDto.INFORMASJONSMOTE,
    ArenaDeltakerStatusDto.TAKKET_JA_TIL_TILBUD,
    ArenaDeltakerStatusDto.TILBUD,
    ArenaDeltakerStatusDto.VENTELISTE,
)
