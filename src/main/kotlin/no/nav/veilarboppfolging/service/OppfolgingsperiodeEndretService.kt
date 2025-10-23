package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.kafka.Tilordningstype.ENDRET_KONTOR
import no.nav.veilarboppfolging.kafka.Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrElse

@Service
class OppfolgingsperiodeEndretService(
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val kafkaProducerService: KafkaProducerService
) {

    fun håndterOppfolgingskontorMelding(melding: OppfolgingskontorMelding) {
        val periodeMedStartDato =
            oppfolgingsPeriodeRepository.hentOppfolgingsperiode(melding.oppfolgingsperiodeId.toString())
                .getOrElse { throw RuntimeException("Ugyldig oppfølgingsperiodeId, noe gikk veldig galt, dette skal aldri skje") }

        val gjeldendeOppfolgingsperiode = GjeldendeOppfolgingsperiode(
            oppfolgingsperiodeId = periodeMedStartDato.uuid,
            startTidspunkt = periodeMedStartDato.startDato,
            kontorId = melding.kontorId,
            kontorNavn = melding.kontorNavn,
            aktorId = melding.aktorId,
            ident = melding.ident,
            sisteEndringsType = SisteEndringsType.fromTilordningstype(melding.tilordningstype),
        )

        kafkaProducerService.publiserOppfolgingsperiodeMedKontor(gjeldendeOppfolgingsperiode)
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
    JsonSubTypes.Type(value = AvsluttetOppfolgingsperiode::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class SisteOppfolgingsperiodeDto(
    val oppfolgingsperiodeUuid: UUID,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)

class GjeldendeOppfolgingsperiode(
    val oppfolgingsperiodeId: UUID,
    val startTidspunkt: ZonedDateTime,
    val kontorId: String,
    val kontorNavn: String,
    aktorId: String,
    ident: String,
    sisteEndringsType: SisteEndringsType,
) : SisteOppfolgingsperiodeDto(
    oppfolgingsperiodeId, sisteEndringsType, aktorId, ident
)

class AvsluttetOppfolgingsperiode(
    val oppfolgingsperiodeId: UUID,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime,
    aktorId: String,
    ident: String,
) : SisteOppfolgingsperiodeDto(
    oppfolgingsperiodeId, SisteEndringsType.OPPFOLGING_AVSLUTTET, aktorId, ident
)