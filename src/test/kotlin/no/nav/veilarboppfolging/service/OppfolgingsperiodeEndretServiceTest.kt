package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import org.mockito.ArgumentCaptor
import no.nav.veilarboppfolging.kafka.OppfolgingskontorMelding
import no.nav.veilarboppfolging.kafka.Tilordningstype
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test


class OppfolgingsperiodeEndretServiceTest {

    @Mock
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()

    @Mock
    val kafkaProducerService: KafkaProducerService = mock()

    @Mock
    val aktorOppslagClient: AktorOppslagClient = mock()


    private val oppfolgingsperiodeEndretService = OppfolgingsperiodeEndretService(oppfolgingsPeriodeRepository, kafkaProducerService, aktorOppslagClient)

    @Test
    fun `skal ignorere tom melding`() {
        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(null)

        verifyNoInteractions(kafkaProducerService, oppfolgingsPeriodeRepository, aktorOppslagClient)
    }

    @Test
    fun `Skal sende melding om OPPFOLGING_STARTET når  oppfolgingskontormelding mottas med tilordningstype KONTOR_VED_OPPFOLGINGSPERIODE_START`() {
        val aktorId = "1111111"
        val oppfolgingsperiode = oppfolgingsperiode(aktorId)
        `when`(oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiode.uuid.toString())).thenReturn(Optional.of(oppfolgingsperiode))
        val oppfolgingskontorMelding = oppfolgingskontorMelding(aktorId = aktorId, tilordningstype = Tilordningstype.KONTOR_VED_OPPFOLGINGSPERIODE_START)

        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(oppfolgingskontorMelding)

        val captor = ArgumentCaptor.forClass(SisteOppfolgingsperiodeDto::class.java)
        verify(kafkaProducerService).publiserOppfolgingsperiodeMedKontor(captor.capture())
        val oppfolgingsperiodeMelding = captor.value as GjeldendeOppfolgingsperiode
        assertThat(oppfolgingsperiodeMelding.sisteEndringsType).isEqualTo(SisteEndringsType.OPPFOLGING_STARTET)
        assertThat(oppfolgingsperiodeMelding.aktorId).isEqualTo(oppfolgingskontorMelding.aktorId)
        assertThat(oppfolgingsperiodeMelding.ident).isEqualTo(oppfolgingskontorMelding.ident)
        assertThat(oppfolgingsperiodeMelding.oppfolgingsperiodeUuid).isEqualTo(oppfolgingskontorMelding.oppfolgingsperiodeId)
        assertThat(oppfolgingsperiodeMelding.kontorId).isEqualTo(oppfolgingskontorMelding.kontorId)
        assertThat(oppfolgingsperiodeMelding.kontorNavn).isEqualTo(oppfolgingskontorMelding.kontorNavn)
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