package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import no.nav.veilarboppfolging.config.KafkaProperties
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.service.FilterhendelsePublisher
import org.springframework.stereotype.Service

@Service
class KandidatForUtmeldingKafkaPubliseringService(
    private val filterhendelsePublisher: FilterhendelsePublisher,
    private val kafkaProperties: KafkaProperties,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
) {
    fun publiserOgLoggKafkaMelding(
        utmeldingshendelseId: UUID,
        filterkategoriPersonId: UUID,
        filterhendelse: FilterhendelseRecord,
    ) {
        val topic = kafkaProperties.portefoljeHendelsesfilterTopic
        try {
            val metadata = filterhendelsePublisher.publiser(topic, filterkategoriPersonId.toString(), filterhendelse)
            kandidatForUtmeldingRepository.lagreKafkaPublisering(
                KandidatForUtmeldingKafkaPublisering(
                    utmeldingshendelseId = utmeldingshendelseId,
                    status = KandidatForUtmeldingKafkaPubliseringStatus.SENDT,
                    kafkaTopic = metadata.topic(),
                    kafkaPartition = metadata.partition(),
                    kafkaOffset = metadata.offset(),
                    feilmelding = null,
                )
            )
        } catch (e: Exception) {
            kandidatForUtmeldingRepository.lagreKafkaPublisering(
                KandidatForUtmeldingKafkaPublisering(
                    utmeldingshendelseId = utmeldingshendelseId,
                    status = KandidatForUtmeldingKafkaPubliseringStatus.FEILET,
                    kafkaTopic = topic,
                    kafkaPartition = null,
                    kafkaOffset = null,
                    feilmelding = e.message?.take(1000),
                )
            )
            throw e
        }
    }
}
