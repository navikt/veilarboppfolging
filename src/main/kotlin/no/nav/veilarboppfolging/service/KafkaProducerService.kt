package no.nav.veilarboppfolging.service

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.util.ProducerUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1
import no.nav.pto_schema.kafka.json.topic.onprem.*
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.disable
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.enable
import no.nav.tms.microfrontend.Sensitivitet
import no.nav.veilarboppfolging.config.KafkaProperties
import no.nav.veilarboppfolging.kafka.KvpPeriode
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class KafkaProducerService @Autowired constructor(
    private val authContextHolder: AuthContextHolder,
    private val producerRecordStorage: KafkaProducerRecordStorage,
    private val kafkaProperties: KafkaProperties,
    @param:Value("\${app.kafka.enabled}") private val kafkaEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun publiserValgtOppfolgingsperiode(oppfolgingsperiode: OppfolgingsperiodeDTO) {
        oppfolgingsperiode(oppfolgingsperiode)
    }

    fun publiserOppfolgingsperiode(oppfolgingsperiode: OppfolgingsperiodeDTO) {
        sisteOppfolgingsPeriode(oppfolgingsperiode.toSisteOppfolgingsperiodeDTO())
        oppfolgingsperiode(oppfolgingsperiode)
    }

    fun publiserOppfolgingsAvsluttet(avsluttetHendelse: OppfolgingsAvsluttetHendelseDto) {
        store(kafkaProperties.oppfolgingshendelseV1, avsluttetHendelse.fnr, avsluttetHendelse)
    }

    fun publiserOppfolgingsStartet(oppfolgingsperiodeStartet: OppfolgingStartetHendelseDto) {
        store(kafkaProperties.oppfolgingshendelseV1, oppfolgingsperiodeStartet.fnr, oppfolgingsperiodeStartet)
    }

    private fun sisteOppfolgingsPeriode(sisteOppfolgingsperiodeV1: SisteOppfolgingsperiodeV1) {
        store(
            kafkaProperties.sisteOppfolgingsperiodeTopic,
            sisteOppfolgingsperiodeV1.aktorId,
            sisteOppfolgingsperiodeV1
        )
    }

    private fun oppfolgingsperiode(sisteOppfolgingsperiodeV1: OppfolgingsperiodeDTO) {
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

    fun publiserVisAoMinSideMicrofrontend(aktorId: AktorId, fnr: Fnr) {
        val startMelding = enable(
            fnr.get(),
            "ao-min-side-microfrontend",
            "dab",
            Sensitivitet.SUBSTANTIAL
        ).text()

        store(kafkaProperties.minSideAapenMicrofrontendV1, aktorId.get(), startMelding)
    }

    fun publiserSkjulAoMinSideMicrofrontend(aktorId: AktorId, fnr: Fnr) {
        val stoppMelding = disable(
            fnr.get(),
            "ao-min-side-microfrontend",
            "dab"
        ).text()

        store(kafkaProperties.minSideAapenMicrofrontendV1, aktorId.get(), stoppMelding)
    }

    fun publiserMinSideBeskjed(fnr: Fnr, beskjed: String, lenke: String) {
        val generertVarselId = UUID.randomUUID().toString()
        val kafkaValueJson = VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            varselId = generertVarselId
            sensitivitet = no.nav.tms.varsel.action.Sensitivitet.High
            ident = fnr.get()
            tekster += Tekst(
                spraakkode = "nb",
                tekst = beskjed,
                default = true
            )
            link = lenke
            aktivFremTil = ZonedDateTime.now(ZoneId.of("Z")).plusDays(14)
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
            }
        }
        logger.debug("Publiserer min side beskjed til kafka med varselid ${generertVarselId}")

        store(topic=kafkaProperties.minSideBrukerVarsel,
            key = generertVarselId,
            value = kafkaValueJson)
    }

    private fun store(topic: String, key: String, value: Any) {
        if (kafkaEnabled) {
            val record = ProducerUtils.serializeJsonRecord(ProducerRecord(topic, key, value))
            producerRecordStorage.store(record)
        } else {
            throw RuntimeException("Kafka er disabled, men noe gjør at man forsøker å publisere meldinger")
        }
    }

    private fun store(topic: String, key: String, value: String) {
        if (kafkaEnabled) {
            val record = ProducerUtils.serializeStringRecord(ProducerRecord(topic, key, value))
            producerRecordStorage.store(record)
        } else {
            throw RuntimeException("Kafka er disabled, men noe gjør at man forsøker å publisere meldinger")
        }
    }
}