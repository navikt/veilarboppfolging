package no.nav.veilarboppfolging.repository.entity

import java.time.ZonedDateTime

data class UtmeldingEntity(
    val aktorId: String,
    val iservSiden: ZonedDateTime,
)
