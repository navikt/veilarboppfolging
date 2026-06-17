package no.nav.veilarboppfolging.controller.request

import no.nav.veilarboppfolging.controller.response.Veileder

data class VeilederTilordning(
    val brukerFnr: String,
    val aktoerId: String?,
    val innloggetVeilederId: String?,
    val fraVeilederId: String?,
    val tilVeilederId: String
) {
    // Hjelpefunksjoner for å lette migrering vekk fra Lombok
    fun oppdaterMedInnloggetVeilederId(veilederId: String) = this.copy(innloggetVeilederId = veilederId)
    fun oppdaterMedAktorId(nyAktorId: String) = this.copy(aktoerId = nyAktorId)
}
