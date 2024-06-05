package no.nav.veilarboppfolging.client.amttiltak

import java.util.UUID

data class DeltakerDto(
    val id: UUID,
    val status: DeltakerStatusDto
) {
    private val aktiveStatuser = listOf(
        DeltakerStatusDto.UTKAST_TIL_PAMELDING,
        DeltakerStatusDto.VENTER_PA_OPPSTART,
        DeltakerStatusDto.DELTAR,
        DeltakerStatusDto.SOKT_INN,
        DeltakerStatusDto.VURDERES,
        DeltakerStatusDto.VENTELISTE
    )

    fun erAktiv(): Boolean {
        return status in aktiveStatuser
    }
}

enum class DeltakerStatusDto {
    UTKAST_TIL_PAMELDING, AVBRUTT_UTKAST,
    VENTER_PA_OPPSTART, DELTAR, HAR_SLUTTET, FULLFORT, IKKE_AKTUELL, FEILREGISTRERT,
    SOKT_INN, VURDERES, VENTELISTE, AVBRUTT, PABEGYNT_REGISTRERING
}
