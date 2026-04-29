package no.nav.veilarboppfolging.service

import java.time.LocalDate
import java.util.Optional
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.oppgave.OppgaveClient
import no.nav.veilarboppfolging.client.oppgave.finnFristForFerdigstillingAvOppgave
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class KontaktBrukerServiceTest {
    private val aktorId = AktorId.of("9876")
    private val fnr = Fnr.of("12345678901")
    @Mock
    private lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository

    @Mock
    private lateinit var pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient

    @Mock
    private lateinit var aktorOppslagClient: AktorOppslagClient

    @Mock
    private lateinit var oppgaveClient: OppgaveClient

    @InjectMocks
    private lateinit var kontaktBrukerService: KontaktBrukerService

    @Test
    fun opprettOppgave_under_18_ikke_under_oppfolging_oppretter_oppgave() {
        val forventetFrist = finnFristForFerdigstillingAvOppgave(LocalDate.now().plusDays(2))
        Mockito.`when`(oppgaveClient.opprettOppgave(fnr, aktorId)).thenReturn(forventetFrist)
        Mockito.`when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId)
        Mockito.`when`(oppfolgingsStatusRepository.hentOppfolging(aktorId)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(false))
        )
        Mockito.`when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)).thenReturn(
            FregStatusOgStatsborgerskap(
                fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
                statsborgerskap = listOf("NO"),
                under18 = true,
            )
        )

        val kontaktBrukerDto = kontaktBrukerService.opprettOppgave(fnr)

        Assertions.assertThat(kontaktBrukerDto.frist).isEqualTo(forventetFrist)
    }

    @Test
    fun opprettOppgave_over_18_kaster_feil() {
        Mockito.`when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)).thenReturn(
            FregStatusOgStatsborgerskap(
                fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
                statsborgerskap = listOf("NO"),
                under18 = false,
            )
        )

        assertThrows<IllegalArgumentException> {
            kontaktBrukerService.opprettOppgave(fnr)
        }
    }

    @Test
    fun opprettOppgave_under_oppfolging_kaster_feil() {
        Mockito.`when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId)
        Mockito.`when`(oppfolgingsStatusRepository.hentOppfolging(aktorId)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(true))
        )
        Mockito.`when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)).thenReturn(
            FregStatusOgStatsborgerskap(
                fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
                statsborgerskap = listOf("NO"),
                under18 = true,
            )
        )

        assertThrows<IllegalArgumentException> {
            kontaktBrukerService.opprettOppgave(fnr)
        }
    }
}