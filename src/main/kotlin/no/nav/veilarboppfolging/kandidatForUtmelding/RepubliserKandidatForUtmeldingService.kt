package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class RepubliserKandidatForUtmeldingService(
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun republiserKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        if (sendUtmeldingskandidaterTilObo) {
            transactor.executeWithoutResult { _ ->
                val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
                    .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
                val fnr = aktorOppslagClient.hentFnr(AktorId(aktorId))
                val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)
                val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeId)
                val filterhendelseRecord = if (kandidat != null) {
                    kandidat.tilFilterhendelseRecord(fnr, Operasjon.START)
                } else {
                    val sisteUtmeldingshendelse = kandidatForUtmeldingRepository.hentSisteKandidatForUtmeldingHendelse(oppfolgingsperiodeId)
                        ?: throw IllegalStateException("Fant ingen kandidat for utmelding-hendelser for oppfølgingsperiode $oppfolgingsperiodeId")
                    sisteUtmeldingshendelse.tilFilterhendelseRecord(fnr, Operasjon.STOPP)
                }
                logger.info("Republiserer kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode $oppfolgingsperiodeId")
                kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelseRecord)
            }
        } else {
            logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode $oppfolgingsperiodeId på nytt fordi sending til OBO er togglet av")
        }
    }
}