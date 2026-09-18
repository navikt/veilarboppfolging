package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
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
class RepubliserKandidatForUtmeldingService(
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val filterkategoriRepository: FilterkategoriRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val BATCH_SIZE = 1000

    fun republiserAlleAktiveUtmeldingskandidater() {
        var currentOffset = 0

        while (true) {
            val aktiveKandidater = kandidatForUtmeldingRepository.hentAktiveKandidater(
                offset = currentOffset,
                batchSize = BATCH_SIZE,
            )

            if (aktiveKandidater.isEmpty()) {
                break
            }

            currentOffset += aktiveKandidater.size

            logger.info(
                "Republiserer aktive kandidater for utmelding. CurrentOffset={} BatchSize={}",
                currentOffset,
                aktiveKandidater.size
            )

            aktiveKandidater.forEach {
                republiserKandidatForUtmelding(it)
            }
        }
        logger.info("Ferdig med å republisere alle aktive kandidater for utmelding til OBO")
    }

    fun republiserKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        val aktivKandidat = kandidatForUtmeldingRepository.hentAktivKandidat(oppfolgingsperiodeId)
        if (aktivKandidat != null) {
            republiserKandidatForUtmelding(aktivKandidat)
        } else {
            val fnr = finnFnrForOppfolgingsperiode(oppfolgingsperiodeId)
            val filterkategoriPersonId = filterkategoriRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeId)
            val filterHendelseRecord = FilterhendelseRecord(
                personID = NorskIdent(fnr.get()),
                kategori = Kategori.KANDIDAT_FOR_UTMELDING,
                operasjon = Operasjon.STOPP,
                hendelse = null
            )
            kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterHendelseRecord)
        }
    }

    fun republiserKandidatForUtmelding(kandidat: KandidatForUtmelding) {
        transactor.executeWithoutResult { _ ->
            val fnr = finnFnrForOppfolgingsperiode(kandidat.oppfolgingsperiodeId)
            val filterkategoriPersonId = filterkategoriRepository.hentEllerOpprettFilterhendelseId(kandidat.oppfolgingsperiodeId)
            val filterhendelseRecord = kandidat.sisteHendelse.tilFilterhendelseRecord(fnr)
            logger.info("Republiserer kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode ${kandidat.oppfolgingsperiodeId}")
            kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelseRecord)
        }
    }

    private fun finnFnrForOppfolgingsperiode(oppfolgingsperiodeId: UUID): Fnr {
        val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
            .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
        return aktorOppslagClient.hentFnr(AktorId(aktorId))
    }
}