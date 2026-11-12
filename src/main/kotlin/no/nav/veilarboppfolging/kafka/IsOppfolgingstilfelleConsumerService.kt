package no.nav.veilarboppfolging.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.veilarboppfolging.sykmeldingstatus.SisteSykmeldingstatusService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class IsOppfolgingstilfelleConsumerService(
    val sisteSykmeldingstatusService: SisteSykmeldingstatusService
) {
    fun consumeIsOppfolgingstilfelle(kafkaMelding: ConsumerRecord<String, KafkaOppfolgingstilfellePerson>?) {
        if (kafkaMelding?.value() != null) {
            val oppfolgingstilfellePerson = kafkaMelding.value()
            sisteSykmeldingstatusService.handterMottattIsOppfolgingstilfelle(
                kafkaMelding.key(),
                oppfolgingstilfellePerson,
            )
        }
    }
}

data class KafkaOppfolgingstilfellePerson(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<KafkaOppfolgingstilfelle>,
    val referanseTilfelleBitUuid: String,
    val referanseTilfelleBitInntruffet: OffsetDateTime,
    val dodsdato: LocalDate?,
    val hasGjentakendeSykefravar: Boolean?,
) {
    fun finnAktivtOppfolgingstilfelle(): KafkaOppfolgingstilfelle? {
        return oppfolgingstilfelleList
            .filter { !it.end.isBefore(LocalDate.now()) }
            .maxByOrNull { it.start }
    }

    fun finnOppfolgingstilfelleMedSisteSluttdato(): KafkaOppfolgingstilfelle? {
        return oppfolgingstilfelleList
            .maxByOrNull { it.end }
    }
}

data class KafkaOppfolgingstilfelle(
    val gradertAtTilfelleEnd: Boolean,
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val virksomhetsnummerList: List<String>,
)