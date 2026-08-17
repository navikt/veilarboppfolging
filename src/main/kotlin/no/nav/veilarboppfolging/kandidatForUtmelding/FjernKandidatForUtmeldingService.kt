package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class FjernKandidatForUtmeldingService(
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun fjernKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeId)
        logger.info("Fjerner kandidat for utmelding")
    }
}