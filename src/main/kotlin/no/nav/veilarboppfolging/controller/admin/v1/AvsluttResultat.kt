package no.nav.veilarboppfolging.controller.admin.v1

data class AvsluttResultat(
    val antallAvsluttet: Int,
    val antallKunneIkkeAvsluttes: Int
)
