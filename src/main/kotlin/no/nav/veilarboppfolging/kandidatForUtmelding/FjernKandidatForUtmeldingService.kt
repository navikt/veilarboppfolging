package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class FjernKandidatForUtmeldingService(
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val filterkategoriRepository: FilterkategoriRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun fjernKandidatForUtmelding(
        oppfolgingsperiodeId: UUID,
    ) {
        transactor.executeWithoutResult { _ ->
            logger.info("Fjerner kandidat for utmelding for oppfølgingsperiode $oppfolgingsperiodeId")
            if (!kandidatForUtmeldingRepository.erKandidat(oppfolgingsperiodeId)) {
                logger.info("Kandidat med oppfølgingsperiodeId $oppfolgingsperiodeId er ikke kandidat for utmelding, ignorerer")
                return@executeWithoutResult
            }

            val filterkategoriPersonId = filterkategoriRepository.hentFilterhendelseId(oppfolgingsperiodeId) ?: return@executeWithoutResult
            val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
                .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
            val fnr = aktorOppslagClient.hentFnr(AktorId(aktorId))
            logger.info("Sender stopp-melding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode $oppfolgingsperiodeId")
            val filterhendelse = FilterhendelseRecord(
                personID = NorskIdent(fnr.get()),
                kategori = Kategori.KANDIDAT_FOR_UTMELDING,
                operasjon = Operasjon.STOPP,
                hendelse = null
            )
            kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelse)
            kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeId)
        }
    }

    fun erOppfolgingForlenget(oppfolgingsperiodeId: UUID): Boolean {
        return kandidatForUtmeldingRepository.hentKandidatMedIkkeUtloptForlengelse(oppfolgingsperiodeId) != null
    }
}
