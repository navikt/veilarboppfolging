package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OppfolgingsperiodeEndretService(
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    val kafkaProducerService: KafkaProducerService
) {

    fun lol(melding: OppfolgingskontorMelding) {
        val periodeMedStartDato = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(melding.oppfolgingsperiodeId.toString())
        kafkaProducerService.publiserOppfolgingsperiodeMedKontor(null)
    }

}

data class OppfolgingskontorMelding(
    val kontorId: String,
    val kontorNavn: String,
    val oppfolgingsperiodeId: UUID,
    val aktorId: String,
    val ident: String
)

enum class SisteEndringsType {
    OPPFOLGING_STARTET,
    ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
    OPPFOLGING_AVSLUTTET
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiode::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = AvsluttetOppfolgingsperiode::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class SisteOppfolgingsperiodeDto(
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
): SisteOppfolgingsperiodeDto(
    SisteEndringsType.OPPFOLGING_STARTET, aktorId, ident
)

class AvsluttetOppfolgingsperiode(
    val oppfolgingsperiodeId: UUID,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime,
    aktorId: String,
    ident: String,
): SisteOppfolgingsperiodeDto(
    SisteEndringsType.OPPFOLGING_AVSLUTTET,aktorId, ident
)