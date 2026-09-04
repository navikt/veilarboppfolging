package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.KafkaProducerService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import kotlin.jvm.optionals.getOrElse

@Service
class FjernKandidatForUtmeldingService(
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun fjernKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        transactor.executeWithoutResult { _ ->
            logger.info("Fjerner kandidat for utmelding for oppfølgingsperiode $oppfolgingsperiodeId")
            if (sendUtmeldingskandidaterTilObo) run sendTilObo@{
                val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId) ?: return@executeWithoutResult
                val filterkategoriPersonId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeId) ?: return@sendTilObo
                val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
                    .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
                val fnr = aktorOppslagClient.hentFnr(AktorId(aktorId))
                logger.info("Sender stopp-melding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode $oppfolgingsperiodeId")
                val hendelse = OppfolgingAvsluttetHendelse(oppfolgingsperiodeId, oppfolgingAvsluttetHendelseType = OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK)
                val filterhendelse = hendelse.tilFilterhendelseRecord(fnr)
                kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelse)
            }
            kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeId)
        }
    }

    fun erOppfolgingForlenget(oppfolgingsperiodeId: UUID): Boolean {
        return kandidatForUtmeldingRepository.hentKandidatMedIkkeUtloptForlengelse(oppfolgingsperiodeId) != null
    }
}
