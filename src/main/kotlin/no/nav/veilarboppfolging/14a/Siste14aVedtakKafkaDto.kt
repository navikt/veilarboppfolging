package no.nav.veilarboppfolging.`14a`

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Siste14aVedtakKafkaDto(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
)

enum class Innsatsgruppe {
    STANDARD_INNSATS,
    SITUASJONSBESTEMT_INNSATS,
    SPESIELT_TILPASSET_INNSATS,
    GRADERT_VARIG_TILPASSET_INNSATS,
    VARIG_TILPASSET_INNSATS
}

enum class HovedmalMedOkeDeltakelse {
    SKAFFE_ARBEID,
    BEHOLDE_ARBEID,
    OKE_DELTAKELSE
}