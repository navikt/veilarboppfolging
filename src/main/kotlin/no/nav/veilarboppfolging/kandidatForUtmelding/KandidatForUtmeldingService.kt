package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
import java.time.ZonedDateTime

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun handterUtmeldingsHendelse(fnr: Fnr, hendelse: KandidatForUtmeldingHendelse) {
        transactor.executeWithoutResult { _ ->
            val avslutningsstatus by lazy { avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr) }
            val erHendelseSomSkalTaPersonInnIFilteret = hendelse is ArbeidssøkerPeriodeAvsluttet
                    || hendelse is ForlengelseUtløptHendelse
            if (erHendelseSomSkalTaPersonInnIFilteret && !avslutningsstatus.kanAvslutte) {
                logger.info("Kandidat kunne ikke avsluttes selvom ${hendelse::class.simpleName}, oppfølgingsperiode ${hendelse.oppfolgingsperiodeUuid}")
                kandidatForUtmeldingRepository.fjernKandidat(hendelse.oppfolgingsperiodeUuid)
                return@executeWithoutResult
            }

            when (hendelse) {
                is ArbeidssøkerPeriodeAvsluttet,
                is ForlengelseUtløptHendelse,
                is ForlengelseOpprettetEllerEndretHendelse -> {
                    val kandidat = KandidatForUtmelding.fromHendelse(hendelse)
                    kandidatForUtmeldingRepository.lagreKandidat(kandidat)
                }
                is OppfolgingAvsluttetHendelse -> {
                    kandidatForUtmeldingRepository.fjernKandidat(hendelse.oppfolgingsperiodeUuid)
                }
            }

            sendUtmeldingskandidatTilObo(hendelse, fnr)
        }
    }

    fun hentKandidatForUtmeldingTag(oppfolgingsperiodeId: UUID): KandidatForUtmeldingTagDto? {
        return kandidatForUtmeldingRepository.hentAktivKandidat(oppfolgingsperiodeId)?.sisteHendelse?.mapTilTag()
    }

    fun hentKandidatForUtmeldingTag(aktorId: AktorId): KandidatForUtmeldingTagDto? {
        val oppfolgingsperiodeId =
            oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)?.getOrNull()?.uuid ?: return null
        return hentKandidatForUtmeldingTag(oppfolgingsperiodeId)
    }

    fun hentUtmeldingsKandidatHendelser(aktorId: AktorId): List<KandidatForUtmeldingHendelse> {
        return kandidatForUtmeldingRepository.hentAlleKandidatForUtmeldingHendelser(aktorId)
    }

    fun hentAktivForlengelse(oppfolgingsperiodeId: UUID): ForlengelseOpprettetEllerEndretHendelse? {
        return kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeId)?.forlengelseHendelse
    }

    fun behandleKandidaterMedUtloptForlengelse() {
        val kandidaterMedUtloptForlengelse = kandidatForUtmeldingRepository.hentKandidaterMedUtloptForlengelse()
        logger.info("Behandler ${kandidaterMedUtloptForlengelse.size} kandidater med utløpt forlengelse")

        kandidaterMedUtloptForlengelse.forEach { kandidat ->
            transactor.executeWithoutResult { _ ->
                val (fnr) = finnFnrForOppfolgingsperiode(kandidat.oppfolgingsperiodeUuid)
                val now = ZonedDateTime.now().toInstant()
                val utløptHendelse = ForlengelseUtløptHendelse(kandidat.oppfolgingsperiodeUuid, now)
                handterUtmeldingsHendelse(fnr, utløptHendelse)
            }
        }
        logger.info("Ferdig med å behandle kandidater med utløpt forlengelse")
    }

    private fun sendUtmeldingskandidatTilObo(kandidat: KandidatForUtmeldingHendelse, fnr: Fnr) {
        if (sendUtmeldingskandidaterTilObo) {
            val filterkategoriPersonId =
                kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(kandidat.oppfolgingsperiodeUuid)
            logger.info("Sender kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode ${kandidat.oppfolgingsperiodeUuid}")
            val filterhendelse = kandidat.tilFilterhendelseRecord(fnr)
            kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelse)
        } else {
            logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode ${kandidat.oppfolgingsperiodeUuid} fordi sending til OBO er togglet av")
        }
    }

    private fun finnFnrForOppfolgingsperiode(oppfolgingsperiodeId: UUID): Pair<Fnr, AktorId> {
        val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
            .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
        return aktorOppslagClient.hentFnr(AktorId(aktorId)) to AktorId(aktorId)
    }

    fun forlengKandidat(hendelse: ForlengelseOpprettetEllerEndretHendelse, fnr: Fnr) {
        logger.info("Lagrer forlengelse for oppfølgingsperiode ${hendelse.oppfolgingsperiodeUuid}")
        handterUtmeldingsHendelse(fnr, hendelse)
    }

    fun hentForlengelseType(oppfolgingsperiodeId: UUID): ForlengelseHendelseType {
        val hendelseType =
            kandidatForUtmeldingRepository.hentSisteHendelseForAktivKandidat(oppfolgingsperiodeId)?.type
                ?: throw IllegalStateException("Fant ingen kandidat for utmelding-hendelser for oppfølgingsperiode $oppfolgingsperiodeId")
        return if (hendelseType == ForlengelseHendelseType.FORLENGELSE_OPPRETTET || hendelseType == ForlengelseHendelseType.FORLENGELSE_ENDRET) {
            ForlengelseHendelseType.FORLENGELSE_ENDRET
        } else {
            ForlengelseHendelseType.FORLENGELSE_OPPRETTET
        }
    }
}