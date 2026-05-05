package no.nav.veilarboppfolging.service

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
import org.junit.Test
import org.mockito.kotlin.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import tools.jackson.databind.JsonNode


class OppfolgingsperiodeEndretServiceTest {

    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()
    val kafkaProducerService: KafkaProducerService = mock()
    val aktorOppslagClient: AktorOppslagClient = mock()
    val ArbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository = mock()

    private val arbeidsoppfolgingsKontorEndretService = ArbeidsoppfolgingsKontorEndretService(oppfolgingsPeriodeRepository, kafkaProducerService, aktorOppslagClient, ArbeidsoppfolgingskontorRepository)

    @Test
    fun `skal ignorere tom arbeidsOppfolgingsKontortilordning-melding`() {
        val internAoPersonIdent = 8L
        val aktorId = "1111111"
        val fnr = Fnr.of("3131")
        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        val personIdentCaptor = argumentCaptor<Long>()
        val oppfolgingsperiode = oppfolgingsperiode(aktorId, oppfolgingsperiodeSlutt = ZonedDateTime.now())
        whenever(oppfolgingsPeriodeRepository.hentSisteOppfolgingsperiodeForAoInternId(internAoPersonIdent)).thenReturn(Optional.of(oppfolgingsperiode.uuid))
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        whenever(aktorOppslagClient.hentFnr(AktorId.of(aktorId))).thenReturn(fnr)
        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(8L, null)

        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture(), personIdentCaptor.capture())
        assertThat(personIdentCaptor.lastValue).isEqualTo(internAoPersonIdent)
        assertThat(captor.lastValue.sluttTidspunkt).isNotNull()
        assertThat(captor.lastValue.sisteEndringsType).isEqualTo(SisteEndringsType.OPPFOLGING_AVSLUTTET)
        assertThat(captor.lastValue.ident).isEqualTo(fnr.get())
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_STARTET når oppfolgingskontormelding mottas med tilordningstype KONTOR_VED_OPPFOLGINGSPERIODE_START`() {
        val aktorId = "1111111"
        val internAoPersonIdent = 8L
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(internAoPersonIdent, oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        val personIdentCaptor = argumentCaptor<Long>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture(), personIdentCaptor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(personIdentCaptor.lastValue).isEqualTo(internAoPersonIdent)
        assertThat(jsonNode["sisteEndringsType"].asString()).isEqualTo("OPPFOLGING_STARTET")
        assertThat(jsonNode["aktorId"].asString()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asString()).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asString()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].isNull)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontor"]["kontorId"].asString()).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(jsonNode["kontor"]["kontorNavn"].asString()).isEqualTo(oppfolgingskontorMelding.kontorNavn)
    }

    @Test
    fun `Skal sende melding med type ARBEIDSOPPFOLGINGSKONTOR_ENDRET når oppfolgingskontormelding mottas med tilordningstype ENDRET_KONTOR`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        val internAoPersonIdent = 8L
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.ENDRET_KONTOR, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        arbeidsoppfolgingsKontorEndretService.håndterOppfolgingskontorMelding(internAoPersonIdent, oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        val personIdentCaptor = argumentCaptor<Long>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture(), personIdentCaptor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(personIdentCaptor.lastValue).isEqualTo(internAoPersonIdent)
        assertThat(jsonNode["sisteEndringsType"].asString()).isEqualTo("ARBEIDSOPPFOLGINGSKONTOR_ENDRET")
        assertThat(jsonNode["aktorId"].asString()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asString()).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asString()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].isNull)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontor"]["kontorId"].asString()).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(jsonNode["kontor"]["kontorNavn"].asString()).isEqualTo(oppfolgingskontorMelding.kontorNavn)
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_AVSLUTTET når oppfølgingsperiode avsluttes`() {
        val aktorId = "1111111"
        val fnr = Fnr("01015054321")
        val startTidspunkt = ZonedDateTime.now().minusDays(10)
        val sluttTidspunkt = ZonedDateTime.now()
        val oppfolgingsperiode = oppfolgingsperiode(aktorId, startTidspunkt, sluttTidspunkt)
        val internAoPersonIdent = 8L
        whenever(aktorOppslagClient.hentFnr(AktorId.of(aktorId))).thenReturn(fnr)

        arbeidsoppfolgingsKontorEndretService.publiserSisteOppfolgingsperiodeV2MedAvsluttetStatus(oppfolgingsperiode, internAoPersonIdent)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        val personIdentCaptor = argumentCaptor<Long>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture(), personIdentCaptor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as AvsluttetOppfolgingsperiode
        val meldingAsJson = JsonUtils.toJson(oppfolgingsperiodeMelding)
        val jsonNode = JsonUtils.getMapper().readTree(meldingAsJson)
        assertThat(personIdentCaptor.lastValue).isEqualTo(internAoPersonIdent)
        assertThat(jsonNode["sisteEndringsType"].asString()).isEqualTo("OPPFOLGING_AVSLUTTET")
        assertThat(jsonNode["aktorId"].asString()).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(jsonNode["ident"].asString()).isEqualTo(fnr.get())
        assertThat(jsonNode["oppfolgingsperiodeUuid"].asString()).isEqualTo(oppfolgingsperiode.uuid.toString())
        assertThat(jsonNode["startTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(jsonNode["sluttTidspunkt"].asZonedDateTime()).isEqualTo(oppfolgingsperiode.sluttDato)
        assertThat(jsonNode["producerTimestamp"].asZonedDateTime()).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(jsonNode["kontor"].isNull)
    }

    fun JsonNode.asZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.parse(this.asString())
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