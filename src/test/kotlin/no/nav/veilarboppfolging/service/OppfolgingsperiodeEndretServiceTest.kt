package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


class OppfolgingsperiodeEndretServiceTest {

    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()
    val kafkaProducerService: KafkaProducerService = mock()
    val aktorOppslagClient: AktorOppslagClient = mock()
    val ArbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository = mock()

    private val arbeidsoppfolgingsKontorEndretService = ArbeidsoppfolgingsKontorEndretService(oppfolgingsPeriodeRepository, kafkaProducerService, aktorOppslagClient, ArbeidsoppfolgingskontorRepository)

    @Ignore("Midlertidig deaktivert for å unngå at vi misser sluttmeldinger ut på nytt topic")
    @Test
    fun `skal ignorere tom oppfolgingskontormelding`() {
        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(UUID.randomUUID().toString(), null)
        verifyNoInteractions(kafkaProducerService, oppfolgingsPeriodeRepository, aktorOppslagClient)
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_STARTET når oppfolgingskontormelding mottas med tilordningstype KONTOR_VED_OPPFOLGINGSPERIODE_START`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(oppfolgingsperiode.uuid.toString(), oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(jsonNode["sisteEndringsType"].asText()).isEqualTo("OPPFOLGING_STARTET")
        assertThat(jsonNode["aktorId"].asText()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asText()).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asText()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].isNull)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontorId"].asText()).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(jsonNode["kontorNavn"].asText()).isEqualTo(oppfolgingskontorMelding.kontorNavn)
    }

    @Test
    fun `Skal sende melding med type ARBEIDSOPPFOLGINGSKONTOR_ENDRET når oppfolgingskontormelding mottas med tilordningstype ENDRET_KONTOR`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.ENDRET_KONTOR, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(oppfolgingsperiode.uuid.toString(), oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(jsonNode["sisteEndringsType"].asText()).isEqualTo("ARBEIDSOPPFOLGINGSKONTOR_ENDRET")
        assertThat(jsonNode["aktorId"].asText()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asText()).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asText()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].isNull)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontorId"].asText()).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(jsonNode["kontorNavn"].asText()).isEqualTo(oppfolgingskontorMelding.kontorNavn)
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_AVSLUTTET når oppfølgingsperiode avsluttes`() {
        val aktorId = "1111111"
        val fnr = Fnr("01015054321")
        val startTidspunkt = ZonedDateTime.now().minusDays(10)
        val sluttTidspunkt = ZonedDateTime.now()
        val oppfolgingsperiode = oppfolgingsperiode(aktorId, startTidspunkt, sluttTidspunkt)
        whenever(aktorOppslagClient.hentFnr(AktorId.of(aktorId))).thenReturn(fnr)

        arbeidsoppfolgingsKontorEndretService.publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as AvsluttetOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(jsonNode["sisteEndringsType"].asText()).isEqualTo("OPPFOLGING_AVSLUTTET")
        assertThat(jsonNode["aktorId"].asText()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asText()).isEqualTo(fnr.get())
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asText()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.sluttDato)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontorId"].isNull)
        assertThat(jsonNode["kontorNavn"].isNull)
    }

    fun JsonNode.asZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.parse(this.asText())
    }

    private fun oppfolgingsperiode(aktorId: String, oppfolgingsperiodeStart: ZonedDateTime = ZonedDateTime.now(), oppfolgingsperiodeSlutt: ZonedDateTime? = null): OppfolgingsperiodeEntity {
        return OppfolgingsperiodeEntity(
            UUID.randomUUID(),
            aktorId,
            null,
            oppfolgingsperiodeStart,
            oppfolgingsperiodeSlutt,
            null,
            emptyList(),
            null,
            "defaultVeileder",
            StartetAvType.VEILEDER
        )
    }

    private fun oppfolgingskontorMelding(aktorId: String, tilordningstype: Tilordningstype, oppfolgingsperiodeId: UUID = UUID.randomUUID()): OppfolgingskontorMelding {
        return OppfolgingskontorMelding(
            kontorId = "1234",
            kontorNavn = "NAV Test",
            oppfolgingsperiodeId = oppfolgingsperiodeId,
            aktorId = aktorId,
            ident = "ident",
            tilordningstype = tilordningstype
        )
    }
}