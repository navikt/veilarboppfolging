package no.nav.veilarboppfolging.kafka

import java.time.ZonedDateTime


data class KvpStartet(
    var opprettetAv: String,
    var opprettetDato: ZonedDateTime,
    var opprettetBegrunnelse: String?,
)
