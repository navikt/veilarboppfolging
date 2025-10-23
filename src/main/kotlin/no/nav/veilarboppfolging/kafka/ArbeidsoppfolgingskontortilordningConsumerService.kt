package no.nav.veilarboppfolging.kafka

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.HendelseType
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Service
class ArbeidsoppfolgingskontortilordningConsumerService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeKontortilordning(kafkaMelding: ConsumerRecord<String, OppfolgingskontorMelding?>) {
        // Hvis tombStone -> ignorer fordi vi har sendt avslutttetMelding når perioden ble avslutta
        // Hvis ikke tombStone -> publiser melding med oppføgingsperiodeStart
    }
}

data class OppfolgingskontorMelding(
    val kontorId: String,
    val kontorNavn: String,
    val oppfolgingsperiodeId: UUID,
    val aktorId: String,
    val ident: String
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppfolgingStartetHendelseDto::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = OppfolgingsAvsluttetHendelseDto::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class GjeldendeOppfolgingsperiodeDto(
    val hendelseType: HendelseType,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
    val producerAppName: HendelseProducerAppName = HendelseProducerAppName.VEILARBOPPFOLGING,
)

enum class HendelseProducerAppName {
    VEILARBOPPFOLGING,
    AO_OPPFOLGINGSKONTOR
}

data class GjeldendeOppfolgingsperiode(
    val oppfolgingsperiodeId: UUID,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime?,
    val aktorId: String,
    val ident: String,
    val kontorId: String?,
    val kontorNavn: String?,
)
