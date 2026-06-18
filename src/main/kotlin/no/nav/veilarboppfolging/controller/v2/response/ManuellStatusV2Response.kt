package no.nav.veilarboppfolging.controller.v2.response


data class ManuellStatusV2Response(
    val erUnderManuellOppfolging: Boolean,
    val krrStatus: KrrStatus,
) {
    data class KrrStatus(
        val kanVarsles: Boolean,
        val erReservert: Boolean,
    )
}
