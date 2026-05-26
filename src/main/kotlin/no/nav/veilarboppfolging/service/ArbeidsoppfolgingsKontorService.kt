package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import lombok.extern.slf4j.Slf4j
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.kafka.Tilordningstype.ENDRET_KONTOR
import no.nav.veilarboppfolging.kafka.Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Slf4j
@Service
class ArbeidsoppfolgingsKontorService(
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val kafkaProducerService: KafkaProducerService,
    val aktorOppslagClient: AktorOppslagClient,
    val arbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository,
    @Lazy val kvpService: KvpService,
    val norg2Client: Norg2Client,
) {
    private val log = LoggerFactory.getLogger(ArbeidsoppfolgingsKontorService::class.java)

    /**
     * Avsluttet status betyr at kontorId og kontorNavn er nullet ut
     * */
    fun publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode: OppfolgingsperiodeEntity, aoKontorInternPersonId: Long) {
        val ident = aktorOppslagClient.hentFnr(AktorId.of(oppfolgingsperiode.aktorId))
        val gjeldendeOppfolgingsperiode = AvsluttetOppfolgingsperiode(
            oppfolgingsperiodeId = oppfolgingsperiode.uuid,
            startTidspunkt = oppfolgingsperiode.startDato,
            sluttTidspunkt = oppfolgingsperiode.sluttDato,
            aktorId = oppfolgingsperiode.aktorId,
            ident = ident.get(),
        )
        kafkaProducerService.publiserOppfolgingsperiodeMedKontor(gjeldendeOppfolgingsperiode, aoKontorInternPersonId)
    }

    fun håndterOppfolgingskontorMelding(aoKontorInternPersonId: Long, melding: OppfolgingskontorMelding?) {
        // TODO I en overgangsperiode lytter vi heller på tombstone fra ao-oppfolgingskontor
        // val erMeldingSomKanIgnoreres = melding == null
        // if (erMeldingSomKanIgnoreres) return

        if (melding == null) {
            // Det skal finnes en inter-nperson-ident når kontoret har blitt slettet
            val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentSisteOppfolgingsperiodeForAoInternId(aoKontorInternPersonId)
                .getOrElse { throw RuntimeException("Fant ingen oppfølgingsperioder på aoKontorInternPersonId og kan derfor ikke publisere oppfølging avsluttet eller kontor endret melding på siste-oppfølgingsperiode v2/v3 topic, dette skal aldri skje") }
            val oppfolgingsperiode =
                oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
                    .getOrElse { throw RuntimeException("Ugyldig oppfølgingsperiodeId, noe gikk veldig galt, dette skal aldri skje") }
            publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode, aoKontorInternPersonId)
            arbeidsoppfolgingskontorRepository.slettNavKontor(oppfolgingsperiode.uuid)
        } else {
            val periodeId = melding.oppfolgingsperiodeId

            oppfolgingsPeriodeRepository.settInternPersonIdentPåOppfolgingsperiode(aoKontorInternPersonId, periodeId)

            val oppfolgingsperiode = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(periodeId.toString())
                    .getOrElse { throw RuntimeException("Ugyldig oppfølgingsperiodeId, noe gikk veldig galt, dette skal aldri skje") }

            kvpService.avsluttKvpVedEnhetBytte(AktorId.of(melding.aktorId), melding.kontorId)

            val gjeldendeOppfolgingsperiode = GjeldendeOppfolgingsperiode(
                oppfolgingsperiodeId = oppfolgingsperiode.uuid,
                startTidspunkt = oppfolgingsperiode.startDato,
                kontor = KontorDto(
                    melding.kontorNavn,
                    melding.kontorId
                ),
                aktorId = melding.aktorId,
                ident = melding.ident,
                sisteEndringsType = SisteEndringsType.fromTilordningstype(melding.tilordningstype),
            )

            arbeidsoppfolgingskontorRepository.settNavKontor(
                melding.ident,
                melding.aktorId,
                melding.oppfolgingsperiodeId,
                melding.kontorId
            )
            kafkaProducerService.publiserOppfolgingsperiodeMedKontor(gjeldendeOppfolgingsperiode, aoKontorInternPersonId)
        }
    }

    fun hentOppfolgingsEnhet(fnr: Fnr): Oppfolgingsenhet? {
        return hentOppfolgingsEnhetId(fnr)
            ?.let { hentEnhet(it) }
    }

    fun hentOppfolgingsEnhetId(fnr: Fnr): EnhetId? {
        return arbeidsoppfolgingskontorRepository.hentEnhet(fnr)
    }

    private fun hentEnhet(enhetId: EnhetId?): Oppfolgingsenhet? {
        if (enhetId == null) return null
        try {
            val enhetNavn = norg2Client.hentEnhet(enhetId.get()).getNavn()
            return Oppfolgingsenhet(navn = enhetNavn, enhetId = enhetId.get())
        } catch (e: Exception) {
            log.warn("Fant ikke navn på enhet", e)
            return Oppfolgingsenhet("", enhetId.get())
        }
    }
}

enum class SisteEndringsType {
    OPPFOLGING_STARTET,
    ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
    OPPFOLGING_AVSLUTTET;

    companion object {
        fun fromTilordningstype(tilordningstype: Tilordningstype): SisteEndringsType {
            return when (tilordningstype) {
                KONTOR_VED_OPPFOLGINGSPERIODE_START -> OPPFOLGING_STARTET
                ENDRET_KONTOR -> ARBEIDSOPPFOLGINGSKONTOR_ENDRET
            }
        }
    }
}

data class KontorDto(
    val kontorNavn: String,
    val kontorId: String,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiode::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiode::class, name = "ARBEIDSOPPFOLGINGSKONTOR_ENDRET"),
    JsonSubTypes.Type(value = AvsluttetOppfolgingsperiode::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class SisteOppfolgingsperiodeDto(
    val oppfolgingsperiodeUuid: UUID,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime?,
    val kontor: KontorDto?,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)

class GjeldendeOppfolgingsperiode(
    oppfolgingsperiodeId: UUID,
    startTidspunkt: ZonedDateTime,
    kontor: KontorDto,
    aktorId: String,
    ident: String,
    sisteEndringsType: SisteEndringsType,
) : SisteOppfolgingsperiodeDto(
    oppfolgingsperiodeId, sisteEndringsType, aktorId, ident, startTidspunkt, null, kontor
)

class AvsluttetOppfolgingsperiode(
    oppfolgingsperiodeId: UUID,
    startTidspunkt: ZonedDateTime,
    sluttTidspunkt: ZonedDateTime,
    aktorId: String,
    ident: String,
) : SisteOppfolgingsperiodeDto(
    oppfolgingsperiodeId,
    SisteEndringsType.OPPFOLGING_AVSLUTTET,
    aktorId,
    ident,
    startTidspunkt,
    sluttTidspunkt,
    null,
)