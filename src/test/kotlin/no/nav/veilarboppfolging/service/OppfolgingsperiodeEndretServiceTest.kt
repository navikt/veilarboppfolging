package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import java.time.ZonedDateTime
import java.util.*


class OppfolgingsperiodeEndretServiceTest {

    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()
    val kafkaProducerService: KafkaProducerService = mock()
    val aktorOppslagClient: AktorOppslagClient = mock()

    private val oppfolgingsperiodeEndretService = OppfolgingsperiodeEndretService(oppfolgingsPeriodeRepository, kafkaProducerService, aktorOppslagClient)

    @Test
    fun `skal ignorere tom oppfolgingskontormelding`() {
        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(null)

        verifyNoInteractions(kafkaProducerService, oppfolgingsPeriodeRepository, aktorOppslagClient)
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_STARTET når oppfolgingskontormelding mottas med tilordningstype KONTOR_VED_OPPFOLGINGSPERIODE_START`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        assertThat(oppfolgingsperiodeMelding.sisteEndringsType).isEqualTo(SisteEndringsType.OPPFOLGING_STARTET)
        assertThat(oppfolgingsperiodeMelding.aktorId).isEqualTo(oppfolgingskontorMelding.aktorId)
        assertThat(oppfolgingsperiodeMelding.ident).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(oppfolgingsperiodeMelding.oppfolgingsperiodeUuid).isEqualTo(oppfolgingskontorMelding.oppfolgingsperiodeId)
        assertThat(oppfolgingsperiodeMelding.kontorId).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(oppfolgingsperiodeMelding.kontorNavn).isEqualTo(oppfolgingskontorMelding.kontorNavn)
        assertThat(oppfolgingsperiodeMelding.startTidspunkt).isEqualTo(oppfolgingsperiode.startDato)
    }

    @Test
    fun `Skal sende melding med type ARBEIDSOPPFOLGINGSKONTOR_ENDRET når oppfolgingskontormelding mottas med tilordningstype ENDRET_KONTOR`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        whenever(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.ENDRET_KONTOR, oppfolgingsperiodeId = oppfolgingsperiode.uuid)

        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(oppfolgingskontorMelding)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as GjeldendeOppfolgingsperiode
        assertThat(oppfolgingsperiodeMelding.sisteEndringsType).isEqualTo(SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET)
        assertThat(oppfolgingsperiodeMelding.aktorId).isEqualTo(oppfolgingskontorMelding.aktorId)
        assertThat(oppfolgingsperiodeMelding.ident).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(oppfolgingsperiodeMelding.oppfolgingsperiodeUuid).isEqualTo(oppfolgingskontorMelding.oppfolgingsperiodeId)
        assertThat(oppfolgingsperiodeMelding.kontorId).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(oppfolgingsperiodeMelding.kontorNavn).isEqualTo(oppfolgingskontorMelding.kontorNavn)
        assertThat(oppfolgingsperiodeMelding.startTidspunkt).isEqualTo(oppfolgingsperiode.startDato)
    }

    @Test
    fun `Skal sende melding med type OPPFOLGING_AVSLUTTET når oppfølgingsperiode avsluttes`() {
        val aktorId = "1111111"
        val fnr = Fnr("01015054321")
        val startTidspunkt = ZonedDateTime.now().minusDays(10)
        val sluttTidspunkt = ZonedDateTime.now()
        val oppfolgingsperiode = oppfolgingsperiode(aktorId, startTidspunkt, sluttTidspunkt)
        whenever(aktorOppslagClient.hentFnr(AktorId.of(aktorId))).thenReturn(fnr)


        oppfolgingsperiodeEndretService.håndterOppfolgingAvsluttet(oppfolgingsperiode)

        val captor = argumentCaptor<SisteOppfolgingsperiodeDto>()
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.firstValue as AvsluttetOppfolgingsperiode
        assertThat(oppfolgingsperiodeMelding.sisteEndringsType).isEqualTo(SisteEndringsType.OPPFOLGING_AVSLUTTET)
        assertThat(oppfolgingsperiodeMelding.aktorId).isEqualTo(oppfolgingsperiode.aktorId)
        assertThat(oppfolgingsperiodeMelding.ident).isEqualTo(fnr.get())
        assertThat(oppfolgingsperiodeMelding.oppfolgingsperiodeUuid).isEqualTo(oppfolgingsperiode.uuid)
        assertThat(oppfolgingsperiodeMelding.startTidspunkt).isEqualTo(oppfolgingsperiode.startDato)
        assertThat(oppfolgingsperiodeMelding.sluttTidspunkt).isEqualTo(oppfolgingsperiode.sluttDato)
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