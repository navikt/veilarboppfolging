package no.nav.veilarboppfolging.controller.request


data class AktiverArbeidssokerData(
    val fnr: Fnr,
    val innsatsgruppe: Innsatsgruppe,
) {
    data class Fnr(
        val fnr: String
    )
}
