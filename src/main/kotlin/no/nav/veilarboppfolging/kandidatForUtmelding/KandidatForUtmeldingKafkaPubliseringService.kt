package no.nav.veilarboppfolging.kandidatForUtmelding

import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.springframework.stereotype.Service

@Service
class KandidatForUtmeldingKafkaPubliseringService(
    private val kafkaProducerService: KafkaProducerService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val meterRegistry: MeterRegistry,
) {
    fun publiserOgLoggKafkaMelding(
        publiseringstype: KandidatForUtmeldingKafkaPubliseringstype,
        utmeldingshendelseId: UUID,
        filterkategoriPersonId: UUID,
        filterhendelse: FilterhendelseRecord,
    ) {
        val topic = kafkaProducerService.portefoljeHendelsesfilterTopic
        try {
            val metadata = kafkaProducerService.publiserFilterhendelseSynkront(filterkategoriPersonId, filterhendelse)
            kandidatForUtmeldingRepository.lagreKafkaPublisering(
                KandidatForUtmeldingKafkaPublisering(
                    utmeldingshendelseId = utmeldingshendelseId,
                    publiseringstype = publiseringstype,
                    status = KandidatForUtmeldingKafkaPubliseringStatus.SENDT,
                    kafkaTopic = metadata.topic(),
                    kafkaPartition = metadata.partition(),
                    kafkaOffset = metadata.offset(),
                    feilmelding = null,
                )
            )
            meterRegistry.counter(
                "kandidat_for_utmelding_kafka_publisering",
                "publiseringstype",
                publiseringstype.name,
                "status",
                KandidatForUtmeldingKafkaPubliseringStatus.SENDT.name,
            ).increment()
        } catch (e: Exception) {
            kandidatForUtmeldingRepository.lagreKafkaPublisering(
                KandidatForUtmeldingKafkaPublisering(
                    utmeldingshendelseId = utmeldingshendelseId,
                    publiseringstype = publiseringstype,
                    status = KandidatForUtmeldingKafkaPubliseringStatus.FEILET,
                    kafkaTopic = topic,
                    kafkaPartition = null,
                    kafkaOffset = null,
                    feilmelding = e.message?.take(1000),
                )
            )
            meterRegistry.counter(
                "kandidat_for_utmelding_kafka_publisering",
                "publiseringstype",
                publiseringstype.name,
                "status",
                KandidatForUtmeldingKafkaPubliseringStatus.FEILET.name,
            ).increment()
            throw e
        }
    }
}
