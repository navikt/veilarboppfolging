package no.nav.veilarboppfolging.service

import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.util.ProducerUtils
import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1
import no.nav.pto_schema.kafka.json.topic.onprem.*
import no.nav.veilarboppfolging.config.KafkaProperties
import no.nav.veilarboppfolging.kafka.KvpPeriode
import no.nav.veilarboppfolging.kafka.MinSideMicrofrontendMessage
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Slf4j
@Service
class KafkaProducerService @Autowired constructor(
    private val authContextHolder: AuthContextHolder,
    private val producerRecordStorage: KafkaProducerRecordStorage,
    private val kafkaProperties: KafkaProperties,
    @param:Value("\${app.kafka.enabled}") private val kafkaEnabled: Boolean, private val authService: AuthService
) {
    fun publiserOppfolgingsperiode(oppfolgingsperiode: OppfolgingsperiodeDTO) {
        sisteOppfolgingsPeriode(oppfolgingsperiode.toSisteOppfolgingsperiodeDTO())
        oppfolingsperiode(oppfolgingsperiode)
    }

    private fun sisteOppfolgingsPeriode(sisteOppfolgingsperiodeV1: SisteOppfolgingsperiodeV1) {
        store(
            kafkaProperties.sisteOppfolgingsperiodeTopic,
            sisteOppfolgingsperiodeV1.aktorId,
            sisteOppfolgingsperiodeV1
        )
    }

    private fun oppfolingsperiode(sisteOppfolgingsperiodeV1: OppfolgingsperiodeDTO) {
        store(
            kafkaProperties.oppfolgingsperiodeTopic,
            sisteOppfolgingsperiodeV1.aktorId,
            sisteOppfolgingsperiodeV1
        )
    }

    fun publiserSisteTilordnetVeileder(recordValue: SisteTilordnetVeilederV1) {
        store(kafkaProperties.sisteTilordnetVeilederTopic, recordValue.aktorId, recordValue)
    }

    fun publiserEndringPaManuellStatus(aktorId: AktorId, erManuell: Boolean) {
        val recordValue = EndringPaManuellStatusV1(aktorId.get(), erManuell)
        store(kafkaProperties.endringPaManuellStatusTopic, recordValue.aktorId, recordValue)
    }

    fun publiserEndringPaNyForVeileder(aktorId: AktorId, erNyForVeileder: Boolean) {
        val recordValue = EndringPaNyForVeilederV1(aktorId.get(), erNyForVeileder)
        store(kafkaProperties.endringPaNyForVeilederTopic, aktorId.get(), recordValue)
    }

    fun publiserVeilederTilordnet(aktorId: AktorId, tildeltVeilederId: String?) {
        val recordValue = VeilederTilordnetV1(aktorId.get(), tildeltVeilederId)
        store(kafkaProperties.veilederTilordnetTopic, aktorId.get(), recordValue)
    }

    fun publiserKvpStartet(
        aktorId: AktorId,
        enhetId: String?,
        opprettetAvVeilederId: String?,
        begrunnelse: String?,
        startDato: ZonedDateTime?
    ) {
        val recordValue = KvpStartetV1.builder()
            .aktorId(aktorId.get())
            .enhetId(enhetId)
            .opprettetAv(opprettetAvVeilederId)
            .opprettetBegrunnelse(begrunnelse)
            .opprettetDato(startDato)
            .build()

        store(kafkaProperties.kvpStartetTopicAiven, aktorId.get(), recordValue)
    }

    fun publiserKvpAvsluttet(aktorId: AktorId, avsluttetAv: String?, begrunnelse: String?, sluttDato: ZonedDateTime?) {
        val recordValue = KvpAvsluttetV1.builder()
            .aktorId(aktorId.get())
            .avsluttetAv(avsluttetAv) // veilederId eller System
            .avsluttetBegrunnelse(begrunnelse)
            .avsluttetDato(sluttDato)
            .build()

        store(kafkaProperties.kvpAvsluttetTopicAiven, aktorId.get(), recordValue)
    }

    fun publiserKvpPeriode(kvpPeriode: KvpPeriode) {
        store(kafkaProperties.kvpPerioderTopicAiven, kvpPeriode.aktorId, kvpPeriode)
    }

    fun publiserEndretMal(aktorId: AktorId, veilederIdent: String?) {
        val recordValue = EndringPaMalV1.builder()
            .aktorId(aktorId.get())
            .endretTidspunk(ZonedDateTime.now())
            .veilederIdent(veilederIdent)
            .lagtInnAv(
                if (authContextHolder.erEksternBruker())
                    EndringPaMalV1.InnsenderData.BRUKER
                else
                    EndringPaMalV1.InnsenderData.NAV
            )
            .build()

        store(kafkaProperties.endringPaMalAiven, aktorId.get(), recordValue)
    }

    fun publiserVisMinSideMicrofrontend(aktorId: AktorId, microfrontend: String) {
        val fnr = authService.getFnrOrThrow(aktorId)

        val visAoMinSideMicrofrontendStartMelding =
            MinSideMicrofrontendMessage("enable", fnr.get(), "substantial", microfrontend)

        store(kafkaProperties.minSideAapenMicrofrontendV1, aktorId.get(), visAoMinSideMicrofrontendStartMelding)
    }

    fun publiserSkjulMinSideMicrofrontend(aktorId: AktorId, microfrontend: String) {
        val fnr = authService.getFnrOrThrow(aktorId)

        val skjulAoMinSideMicrofrontendStartMelding =
            MinSideMicrofrontendMessage("disable", fnr.get(), "substantial", microfrontend)

        store(kafkaProperties.minSideAapenMicrofrontendV1, aktorId.get(), skjulAoMinSideMicrofrontendStartMelding)
    }

    private fun store(topic: String, key: String, value: Any) {
        if (kafkaEnabled) {
            val record = ProducerUtils.serializeJsonRecord(ProducerRecord(topic, key, value))
            producerRecordStorage.store(record)
        } else {
            throw RuntimeException("Kafka er disabled, men noe gjør at man forsøker å publisere meldinger")
        }
    }
}