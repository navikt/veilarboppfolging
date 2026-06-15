package no.nav.veilarboppfolging.kafka

import java.time.ZonedDateTime


data class KvpAvsluttet(
    val avsluttetAv: String,
    val avsluttetDato: ZonedDateTime,
    val avsluttetBegrunnelse: String?,
)