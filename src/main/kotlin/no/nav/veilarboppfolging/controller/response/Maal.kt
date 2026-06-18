package no.nav.veilarboppfolging.controller.response

import java.time.ZonedDateTime


data class Maal(
    val mal: String?,
    val endretAv: String?,
    val dato: ZonedDateTime?,
)