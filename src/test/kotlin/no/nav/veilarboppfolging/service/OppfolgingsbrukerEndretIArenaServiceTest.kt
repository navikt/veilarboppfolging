package no.nav.veilarboppfolging.service

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Optional
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KanAvsluttesInput
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneAvsluttes
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class OppfolgingsbrukerEndretIArenaServiceTest {

    private val oppfolgingService: OppfolgingService = mock(OppfolgingService::class.java)
    private val avsluttOppfolgingService: AvsluttOppfolgingService = mock(AvsluttOppfolgingService::class.java)
    private val startOppfolgingService: StartOppfolgingService = mock(StartOppfolgingService::class.java)
    private val arenaOppfolgingService: ArenaOppfolgingService = mock(ArenaOppfolgingService::class.java)
    private val metricsService: MetricsService = mock(MetricsService::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository = mock(OppfolgingsStatusRepository::class.java)
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient = mock(PdlFolkeregisterStatusClient::class.java)

    val oppfolgingsbrukerEndretIArenaService = OppfolgingsbrukerEndretIArenaService(
        oppfolgingService,
        avsluttOppfolgingService,
        startOppfolgingService,
        arenaOppfolgingService,
        metricsService,
        oppfolgingsStatusRepository,
        pdlFolkeregisterStatusClient,
    )

    val AKTOR_ID = AktorId("0102030405")
    val FNR = Fnr("1102030405")

    @Test
    fun `skal ikke avslutte brukere som kan reaktiveres i Arena`() {
        oppfolgingStatus(underOppfolging = true)
        kanReaktiveres()
        val melding = meldingFraArena(Formidlingsgruppe.ISERV, Kvalifiseringsgruppe.BATT)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(avsluttOppfolgingService, never()).avsluttOppfolgingHvisKanAvsluttes(any())
    }

    @Test
    fun `skal avslutte brukere som ikke kan reaktiveres i Arena`() {
        oppfolgingStatus(underOppfolging = true)
        kanIkkeReaktiveres()
        kanAvsluttes()
        val melding = meldingFraArena(Formidlingsgruppe.ISERV, Kvalifiseringsgruppe.BATT)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(avsluttOppfolgingService, times(1)).avsluttOppfolgingHvisKanAvsluttes(any())
    }

    @Test
    fun `skal starte oppfølging på brukere som ble sykmeldt uten arbeidsgiver`() {
        oppfolgingStatus(underOppfolging = false)
        kanIkkeReaktiveres()
        kanAvsluttes()
        brukerSomErOver18()
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VURDU)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, times(1))
            .startOppfolgingHvisIkkeAlleredeStartet(any())
    }

    @Test
    fun `skal ikke starte oppfølging på bruker under 18 som ble sykmeldt uten arbeidsgiver`() {
        oppfolgingStatus(underOppfolging = false)
        kanIkkeReaktiveres()
        kanAvsluttes()
        brukerSomErUnder18()
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VURDU)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
    }

    @ParameterizedTest
    @EnumSource(Kvalifiseringsgruppe::class, names = ["IKVAL", "BATT", "BFORM", "VARIG"])
    fun `14a vedtak i arena skal ikke starte oppfølging`(kvalifiseringsgruppe: Kvalifiseringsgruppe) {
        oppfolgingStatus(underOppfolging = false)
        kanAvsluttes()
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(avsluttOppfolgingService, never())
            .avsluttOppfolgingHvisKanAvsluttes(any())
    }

    @ParameterizedTest
    @EnumSource(Kvalifiseringsgruppe::class, names = ["BKART", "IVURD", "VURDI"])
    fun `skal ikkke gjøre noe på brukere under oppfølging som ikke er ISERV`(kvalifiseringsgruppe: Kvalifiseringsgruppe) {
        oppfolgingStatus(underOppfolging = true)
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(avsluttOppfolgingService, never())
            .avsluttOppfolgingHvisKanAvsluttes(any())
    }

    @ParameterizedTest
    @EnumSource(Kvalifiseringsgruppe::class, names = ["BKART", "IVURD", "VURDI", "OPPFI"])
    fun `når bruker registreres i arena skal ikke oppfølging startes automatisk (utenom VURDU)`(kvalifiseringsgruppe: Kvalifiseringsgruppe) {
        oppfolgingStatus(underOppfolging = false)
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(avsluttOppfolgingService, never())
            .avsluttOppfolgingHvisKanAvsluttes(any())
    }

    private fun meldingFraArena(formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe): EndringPaaOppfolgingsBruker {
        return EndringPaaOppfolgingsBruker(
            AKTOR_ID,
            FNR.get(),
            formidlingsgruppe = formidlingsgruppe,
            iservFraDato = null,
            kvalifiseringsgruppe = kvalifiseringsgruppe,
            rettighetsgruppe = null,
            hovedmaal = null,
            sistEndretDato = ZonedDateTime.now(),
        )
    }

    private fun oppfolgingStatus(underOppfolging: Boolean, iservFraDato: LocalDate? = null) {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(
                TestUtils.oppfølgingEntity(aktorId = AKTOR_ID.get(), underOppfolging = underOppfolging, localArenaOppfølging =
                    LocalArenaOppfolging(
                        Hovedmaal.BEHOLDEA,
                        Kvalifiseringsgruppe.BATT,
                        if (iservFraDato != null) Formidlingsgruppe.ISERV else Formidlingsgruppe.IARBS,
                        iservFraDato,
                    )
                )
            )
        )
    }

    private fun kanAvsluttes() {
        `when`(avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(any()))
            .thenReturn(KunneAvsluttes(
                ArenaIservKanIkkeReaktiveres(AKTOR_ID),
                true,
                KanAvsluttesInput(
                    erUnderOppfolging = true,
                    erIservIArena = false,
                    harAktiveTiltaksdeltakelser = false,
                    erDeltakerIUngdomsprogrammet = false,
                    erArbeidssoeker = false,
                    harAap = false,
                    underKvp = false
                )
            ))
    }

    private fun kanReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(FNR)).thenReturn(Optional.of(true))
    }
    private fun kanIkkeReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(FNR)).thenReturn(Optional.of(false))
    }

    private fun brukerSomErOver18() {
        val pdlFolkeregisterStatus = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = listOf("NOR"),
            under18 = false,
        )
        `when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(FNR)).thenReturn(pdlFolkeregisterStatus)
    }

    private fun brukerSomErUnder18() {
        val pdlFolkeregisterStatus = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = listOf("NOR"),
            under18 = true,
        )
        `when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(FNR)).thenReturn(pdlFolkeregisterStatus)
    }
}