package no.nav.veilarboppfolging.repository.entity

import java.time.ZonedDateTime

data class OppfolgingsenhetEndringEntity(
    var enhet: String,
    var endretDato: ZonedDateTime,
)
