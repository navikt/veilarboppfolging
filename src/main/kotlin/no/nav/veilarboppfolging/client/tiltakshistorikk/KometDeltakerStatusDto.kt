package no.nav.veilarboppfolging.client.tiltakshistorikk

data class KometDeltakerStatusDto(
    val type: DeltakerStatusType,
) {
    enum class DeltakerStatusType {
        AVBRUTT,
        AVBRUTT_UTKAST,
        DELTAR,
        FEILREGISTRERT,
        FULLFORT,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        KLADD,
        PABEGYNT_REGISTRERING,
        SOKT_INN,
        UTKAST_TIL_PAMELDING,
        VENTELISTE,
        VENTER_PA_OPPSTART,
        VURDERES,
    }

    fun erAktiv() = this.type in listOf(
        DeltakerStatusType.UTKAST_TIL_PAMELDING,
        DeltakerStatusType.VENTER_PA_OPPSTART,
        DeltakerStatusType.DELTAR,
        DeltakerStatusType.SOKT_INN,
        DeltakerStatusType.VURDERES,
        DeltakerStatusType.VENTELISTE,
    )
}
