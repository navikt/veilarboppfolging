package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
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
    private val kandidatForUtmeldingKafkaPubliseringService: KandidatForUtmeldingKafkaPubliseringService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val BATCH_SIZE = 1000

    fun republiserAlleAktiveUtmeldingskandidater() {
        if (sendUtmeldingskandidaterTilObo) {
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
                    republiserKandidatForUtmelding(it.oppfolgingsperiodeUuid)
                }
            }
            logger.info("Ferdig med å republisere alle aktive kandidater for utmelding til OBO")
        } else {
            logger.info("Sender ikke aktive kandidater for utmelding til OBO på nytt fordi sending til OBO er togglet av")
        }
    }

    fun republiserKandidatForUtmelding(oppfolgingsperiodeId: UUID) {
        if (sendUtmeldingskandidaterTilObo) {
            val publiseringsdata = transactor.execute {
                val fnr = finnFnrForOppfolgingsperiode(oppfolgingsperiodeId)
                val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeId)
                val aktivKandidat = kandidatForUtmeldingRepository.hentAktivKandidat(oppfolgingsperiodeId)
                if (aktivKandidat != null) {
                    val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)
                        ?: throw IllegalStateException("Fant ikke aktiv kandidat for oppfølgingsperiode $oppfolgingsperiodeId")
                    Republiseringsdata(
                        utmeldingshendelseId = aktivKandidat.sisteUtmeldingshendelseId,
                        filterkategoriPersonId = filterkategoriPersonId,
                        filterhendelseRecord = kandidat.tilFilterhendelseRecord(fnr, Operasjon.START),
                    )
                } else {
                    val sisteUtmeldingshendelse = kandidatForUtmeldingRepository.hentSisteKandidatForUtmeldingHendelseMedId(oppfolgingsperiodeId)
                        ?: throw IllegalStateException("Fant ingen kandidat for utmelding-hendelser for oppfølgingsperiode $oppfolgingsperiodeId")
                    Republiseringsdata(
                        utmeldingshendelseId = sisteUtmeldingshendelse.utmeldingshendelseId,
                        filterkategoriPersonId = filterkategoriPersonId,
                        filterhendelseRecord = sisteUtmeldingshendelse.hendelse.tilFilterhendelseRecord(fnr, Operasjon.STOPP),
                    )
                }
            }
            logger.info("Republiserer kandidat for utmelding til OBO med key=${publiseringsdata?.filterkategoriPersonId} for oppfølgingsperiode $oppfolgingsperiodeId")
            publiseringsdata?.let {
                kandidatForUtmeldingKafkaPubliseringService.publiserOgLoggKafkaMelding(
                    utmeldingshendelseId = it.utmeldingshendelseId,
                    filterkategoriPersonId = it.filterkategoriPersonId,
                    filterhendelse = it.filterhendelseRecord,
                )
            }
        } else {
            logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode $oppfolgingsperiodeId på nytt fordi sending til OBO er togglet av")
        }
    }

    private fun finnFnrForOppfolgingsperiode(oppfolgingsperiodeId: UUID): Fnr {
        val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
            .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
        return aktorOppslagClient.hentFnr(AktorId(aktorId))
    }
}

private data class Republiseringsdata(
    val utmeldingshendelseId: UUID,
    val filterkategoriPersonId: UUID,
    val filterhendelseRecord: FilterhendelseRecord,
)
