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
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KandidatUtmeldtEtter28Dager
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneIkkeAvsluttes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import java.time.ZonedDateTime

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val filterkategoriRepository: FilterkategoriRepository,
    private val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    private val utmeldingService: UtmeldingsService,
    @Value("\${app.utmeldingskandidater_aktivert}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val BATCH_SIZE = 1000
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

            val kandidat = KandidatForUtmelding.fromHendelse(hendelse)
            kandidatForUtmeldingRepository.lagreKandidat(kandidat)

            utmeldingService.slettFraUtmeldingTabell(hendelse.oppfolgingsperiodeUuid)
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

    fun erAktivUtmeldingskandidat(aktorId: AktorId): Boolean {
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository
            .hentGjeldendeOppfolgingsperiode(aktorId)?.getOrNull()?.uuid ?: return false
        return erAktivUtmeldingskandidat(oppfolgingsperiodeId)
    }

    fun erAktivUtmeldingskandidat(oppfolgingsperiodeId: UUID): Boolean {
        val kandidat = kandidatForUtmeldingRepository.hentAktivKandidat(oppfolgingsperiodeId)
        return kandidat != null
    }

    fun erAktivEllerForlengetKandidatForUtmelding(oppfolgingsperiodeId: UUID): Boolean {
        return kandidatForUtmeldingRepository.erAktivEllerForlengetKandidatForUtmelding(oppfolgingsperiodeId)
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

    fun avsluttOppfolgingForKandidaterMedPassertAvsluttesAutomatiskDato() {
        if(sendUtmeldingskandidaterTilObo) {
            val kandidaterSomSkalAutomatiskAvsluttes =
                kandidatForUtmeldingRepository.hentKandidaterSomSkalAutomatiskAvsluttes()
            logger.info("Behandler ${kandidaterSomSkalAutomatiskAvsluttes.size} kandidater med passert avsluttes_automatisk_dato")

            kandidaterSomSkalAutomatiskAvsluttes.forEach { kandidat ->
                transactor.executeWithoutResult { _ ->
                    val (_, aktorId) = finnFnrForOppfolgingsperiode(kandidat.oppfolgingsperiodeId)
                    val resultat =
                        avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(KandidatUtmeldtEtter28Dager(aktorId))
                    if (resultat is KunneIkkeAvsluttes) {
                        kandidatForUtmeldingRepository.lagreKandidatSomIkkeKunneAvsluttes(
                            kandidat.oppfolgingsperiodeId,
                            resultat.begrunnelse
                        )
                        fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(kandidat.oppfolgingsperiodeId)
                        logger.info("Kandidat med oppfølgingsperiode ${kandidat.oppfolgingsperiodeId} kunne ikke avsluttes automatisk og ble flyttet ut av aktiv liste")
                    }
                }
            }

            logger.info("Ferdig med å avslutte oppfølging for kandidater med passert avsluttes_automatisk_dato")
        }
    }

    fun fjernKandidaterSomIkkeKanAvsluttes() {
        var currentOffset = 0
        while (true) {
            val alleKandidater = kandidatForUtmeldingRepository.hentAlleKandidater(
                offset = currentOffset,
                batchSize = BATCH_SIZE,
            )
            if (alleKandidater.isEmpty()) {
                break
            }
            currentOffset += alleKandidater.size

            logger.info(
                "Sjekker om kandidater for utmelding fortsatt kan avsluttes. CurrentOffset={} BatchSize={}",
                currentOffset,
                alleKandidater.size
            )

            alleKandidater.forEach { kandidat ->
                transactor.executeWithoutResult { _ ->
                    val (fnr, _) = finnFnrForOppfolgingsperiode(kandidat.oppfolgingsperiodeId)
                    val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr)
                    if (!avslutningsstatus.kanAvslutte) {
                        logger.info("Kandidat med oppfølgingsperiode ${kandidat.oppfolgingsperiodeId} kan ikke avsluttes, fjerner fra kandidat for utmelding")
                        fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(kandidat.oppfolgingsperiodeId)
                        return@executeWithoutResult
                    }
                }
            }
        }
        logger.info("Ferdig med å sjekke om kandidater fortsatt kan avsluttes")
    }

    private fun sendUtmeldingskandidatTilObo(kandidat: KandidatForUtmeldingHendelse, fnr: Fnr) {
        if (sendUtmeldingskandidaterTilObo) {
            val filterkategoriPersonId =
                filterkategoriRepository.hentEllerOpprettFilterhendelseId(kandidat.oppfolgingsperiodeUuid)
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