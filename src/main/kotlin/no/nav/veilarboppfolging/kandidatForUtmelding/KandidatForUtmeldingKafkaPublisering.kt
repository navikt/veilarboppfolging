package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID

data class KandidatForUtmeldingKafkaPublisering(
    val utmeldingshendelseId: UUID,
    val status: KandidatForUtmeldingKafkaPubliseringStatus,
    val kafkaTopic: String,
    val kafkaPartition: Int?,
    val kafkaOffset: Long?,
    val feilmelding: String?,
)

enum class KandidatForUtmeldingKafkaPubliseringStatus {
    SENDT,
    FEILET,
}

data class AktivKandidatForUtmelding(
    val oppfolgingsperiodeUuid: UUID,
    val sisteUtmeldingshendelseId: UUID,
)

data class KandidatForUtmeldingHendelseMedId(
    val utmeldingshendelseId: UUID,
    val hendelse: KandidatForUtmeldingHendelse,
)
