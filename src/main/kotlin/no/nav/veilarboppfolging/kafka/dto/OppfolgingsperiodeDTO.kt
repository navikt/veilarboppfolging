package no.nav.veilarboppfolging.kafka.dto

import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1
import java.time.ZonedDateTime
import java.util.*

data class OppfolgingsperiodeDTO(
    val uuid: String,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?,
    val aktorId: String,
    val startetBegrunnelse: StartetBegrunnelseDTO
) {
    fun toSisteOppfolgingsperiodeDTO(): SisteOppfolgingsperiodeV1 {
        return SisteOppfolgingsperiodeV1(
            UUID.fromString(uuid),
            aktorId,
            startDato,
            sluttDato,
        )
    }
}

enum class StartetBegrunnelseDTO {
    ARBEIDSSOKER,
    SYKEMELDT_MER_OPPFOLGING
}

