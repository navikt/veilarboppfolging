package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.kafka.Tilordningstype.ENDRET_KONTOR
import no.nav.veilarboppfolging.kafka.Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrElse

@Service
class ArbeidsoppfolgingsKontorEndretService(
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val kafkaProducerService: KafkaProducerService,
    val aktorOppslagClient: AktorOppslagClient
) {

    /**
     * Avsluttet status betyr at kontorId og kontorNavn er nullet ut
     * */
    fun publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode: OppfolgingsperiodeEntity) {
        val ident = aktorOppslagClient.hentFnr(AktorId.of(oppfolgingsperiode.aktorId))
        val gjeldendeOppfolgingsperiode = AvsluttetOppfolgingsperiode(
            oppfolgingsperiodeId = oppfolgingsperiode.uuid,
            startTidspunkt = oppfolgingsperiode.startDato,
            sluttTidspunkt = oppfolgingsperiode.sluttDato,
            aktorId = oppfolgingsperiode.aktorId,
            ident = ident.get(),
        )

        kafkaProducerService.publiserOppfolgingsperiodeMedKontor(gjeldendeOppfolgingsperiode)
    }

    fun håndterOppfolgingskontorMelding(oppfolgingsperiodeId: String, melding: OppfolgingskontorMelding?) {
        // TODO I en overgangsperiode lytter vi heller på tombstone fra ao-oppfolgingskontor
        // val erMeldingSomKanIgnoreres = melding == null
        // if (erMeldingSomKanIgnoreres) return

        val oppfolgingsperiode =
            oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId)
                .getOrElse { throw RuntimeException("Ugyldig oppfølgingsperiodeId, noe gikk veldig galt, dette skal aldri skje") }

        if (melding == null) {
            publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode)
        } else {
            val gjeldendeOppfolgingsperiode = GjeldendeOppfolgingsperiode(
                oppfolgingsperiodeId = oppfolgingsperiode.uuid,
                startTidspunkt = oppfolgingsperiode.startDato,
                kontorId = melding.kontorId,
                kontorNavn = melding.kontorNavn,
                aktorId = melding.aktorId,
                ident = melding.ident,
                sisteEndringsType = SisteEndringsType.fromTilordningstype(melding.tilordningstype),
            )

            kafkaProducerService.publiserOppfolgingsperiodeMedKontor(gjeldendeOppfolgingsperiode)
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
    val kontorId: String?,
    val kontorNavn: String?,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)

class GjeldendeOppfolgingsperiode(
    oppfolgingsperiodeId: UUID,
    startTidspunkt: ZonedDateTime,
    kontorId: String,
    kontorNavn: String,
    aktorId: String,
    ident: String,
    sisteEndringsType: SisteEndringsType,
) : SisteOppfolgingsperiodeDto(
    oppfolgingsperiodeId, sisteEndringsType, aktorId, ident, startTidspunkt, null, kontorId, kontorNavn
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
    null
)