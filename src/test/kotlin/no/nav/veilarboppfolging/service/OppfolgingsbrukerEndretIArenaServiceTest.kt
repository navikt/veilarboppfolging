package no.nav.veilarboppfolging.service

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.*

class OppfolgingsbrukerEndretIArenaServiceTest {
    private val authService: AuthService = Mockito.mock(AuthService::class.java)
    private val oppfolgingService: OppfolgingService = Mockito.mock(OppfolgingService::class.java)
    private val startOppfolgingService: StartOppfolgingService = Mockito.mock(StartOppfolgingService::class.java)
    private val arenaOppfolgingService: ArenaOppfolgingService = Mockito.mock(ArenaOppfolgingService::class.java)
    private val kvpService: KvpService = Mockito.mock(KvpService::class.java)
    private val metricsService: MetricsService = Mockito.mock(MetricsService::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository = Mockito.mock(OppfolgingsStatusRepository::class.java)

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    private val oppfolgingsbrukerEndretIArenaService = OppfolgingsbrukerEndretIArenaService(
        oppfolgingService, startOppfolgingService, arenaOppfolgingService,
        kvpService, metricsService, oppfolgingsStatusRepository
    )

    private fun inaktivertBruker(): EndringPaaOppfolgingsBruker {
        return EndringPaaOppfolgingsBruker(
            aktorId =  TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )
    }
    private fun brukerSomErArbeidssøkerIArena(): EndringPaaOppfolgingsBruker {
        return EndringPaaOppfolgingsBruker(
            aktorId =  TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            oppfolgingsenhet = "1234",
            formidlingsgruppe = Formidlingsgruppe.ARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )
    }

    private fun brukerErUnderOppfolgingLokalt() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(Optional.of(OppfolgingEntity()
                .setLocalArenaOppfolging(Optional.empty())
                .setUnderOppfolging(true)))
    }
    private fun brukerErIkkeUnderOppfolgingLokalt() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(Optional.of(OppfolgingEntity().setUnderOppfolging(false)))
    }
    private fun kanReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(true)
        )
    }
    private fun kanIkkeReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(false)
        )
    }

    @BeforeEach
    fun beforeEach() {
        `when`(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
    }

    @Test
    fun `skal ikke oppdatere hvis bruker ikke under oppfolging i veilarboppfolging eller arena`() {
        brukerErIkkeUnderOppfolgingLokalt()
        val oppdateringFraArena = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(oppdateringFraArena)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, never()).avsluttOppfolging(any())
    }

    @Test
    fun `skal ikke starte oppfolging pa bruker som ikke er under oppfolging i veilarboppfolging men arbeidssøker i arena`() {
        brukerErIkkeUnderOppfolgingLokalt()
        val arenaOppdatering = brukerSomErArbeidssøkerIArena()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(arenaOppdatering)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, never()).avsluttOppfolging(any())
    }

    @Test
    fun `skal avslutte oppfolging pa bruker som er under oppfolging i veilarboppfolging men er ISERV i arena og ikke kan reaktiveres`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, times(1))
            .avsluttOppfolging(ArenaIservKanIkkeReaktiveres(TEST_AKTOR_ID))
        verify<MetricsService?>(metricsService, times(1))!!
            .rapporterAutomatiskAvslutningAvOppfolging(true)
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som kan enkelt reaktiveres`() {
        brukerErUnderOppfolgingLokalt()
        kanReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erDeltakerIUngdomsprogrammet(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erArbeidssoeker(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.harAap(TEST_FNR)).thenReturn(false)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, never()).avsluttOppfolging(any())
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er under kvp`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(true)
        `when`(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erDeltakerIUngdomsprogrammet(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erArbeidssoeker(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.harAap(TEST_FNR)).thenReturn(false)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(
            any(OppfolgingsRegistrering::class.java)
        )
        verify(oppfolgingService, never())
            .avsluttOppfolging(any(Avregistrering::class.java))
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som har aktive tiltaksdeltakelser`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(true)
        `when`(oppfolgingService.erDeltakerIUngdomsprogrammet(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erArbeidssoeker(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.harAap(TEST_FNR)).thenReturn(false)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, never()).avsluttOppfolging(any())
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er deltaker i ungdomsprogrammet`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erDeltakerIUngdomsprogrammet(TEST_FNR)).thenReturn(true)
        `when`(oppfolgingService.erArbeidssoeker(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.harAap(TEST_FNR)).thenReturn(false)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(
            any(OppfolgingsRegistrering::class.java)
        )
        verify(oppfolgingService, never())
            .avsluttOppfolging(any(Avregistrering::class.java))
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er arbeidssoeker`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erDeltakerIUngdomsprogrammet(TEST_FNR)).thenReturn(false)
        `when`(oppfolgingService.erArbeidssoeker(TEST_FNR)).thenReturn(true)

        val brukverV2 = inaktivertBruker()

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(oppfolgingService, never()).avsluttOppfolging(any())
    }
}