package no.nav.veilarboppfolging.service

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
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
    private val startOppfolgingService: StartOppfolgingService = mock(StartOppfolgingService::class.java)
    private val arenaOppfolgingService: ArenaOppfolgingService = mock(ArenaOppfolgingService::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository = mock(OppfolgingsStatusRepository::class.java)
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient = mock(PdlFolkeregisterStatusClient::class.java)
    private val kandidatForUtmeldingService: KandidatForUtmeldingService = mock(KandidatForUtmeldingService::class.java)

    val oppfolgingsbrukerEndretIArenaService = OppfolgingsbrukerEndretIArenaService(
        oppfolgingService = oppfolgingService,
        startOppfolgingService = startOppfolgingService,
        arenaOppfolgingService = arenaOppfolgingService,
        oppfolgingsStatusRepository = oppfolgingsStatusRepository,
        pdlFolkeregisterStatusClient = pdlFolkeregisterStatusClient,
        kandidatForUtmeldingService = kandidatForUtmeldingService,
    )

    val AKTOR_ID = AktorId("0102030405")
    val FNR = Fnr("1102030405")

    @Test
    fun `brukere som kan reaktiveres i Arena skal delegeres til kandidatForUtmeldingService`() {
        oppfolgingStatus(underOppfolging = true)
        kanReaktiveres()
        val melding = meldingFraArena(Formidlingsgruppe.ISERV, Kvalifiseringsgruppe.BATT)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(kandidatForUtmeldingService, times(1)).handterUtmeldingsHendelse(any(), any())
    }

    @Test
    fun `brukere som ikke kan reaktiveres i Arena skal delegeres til kandidatForUtmeldingService`() {
        oppfolgingStatus(underOppfolging = true)
        kanIkkeReaktiveres()
        val melding = meldingFraArena(Formidlingsgruppe.ISERV, Kvalifiseringsgruppe.BATT)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(kandidatForUtmeldingService, times(1)).handterUtmeldingsHendelse(any(), any())
    }

    @Test
    fun `skal starte oppfølging på brukere som ble sykmeldt uten arbeidsgiver`() {
        oppfolgingStatus(underOppfolging = false)
        kanIkkeReaktiveres()
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
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(kandidatForUtmeldingService, never()).handterUtmeldingsHendelse(any(), any())
    }

    @ParameterizedTest
    @EnumSource(Kvalifiseringsgruppe::class, names = ["BKART", "IVURD", "VURDI"])
    fun `skal ikke gjøre noe på brukere under oppfølging som ikke er ISERV`(kvalifiseringsgruppe: Kvalifiseringsgruppe) {
        oppfolgingStatus(underOppfolging = true)
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(kandidatForUtmeldingService, never()).handterUtmeldingsHendelse(any(), any())
    }

    @ParameterizedTest
    @EnumSource(Kvalifiseringsgruppe::class, names = ["BKART", "IVURD", "VURDI", "OPPFI"])
    fun `når bruker registreres i arena skal ikke oppfølging startes automatisk (utenom VURDU)`(kvalifiseringsgruppe: Kvalifiseringsgruppe) {
        oppfolgingStatus(underOppfolging = false)
        val melding = meldingFraArena(Formidlingsgruppe.IARBS, kvalifiseringsgruppe)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(startOppfolgingService, never())
            .startOppfolgingHvisIkkeAlleredeStartet(any())
        verify(kandidatForUtmeldingService, never()).handterUtmeldingsHendelse(any(), any())
    }

    @Test
    fun `skal ignorere brukere som går fra ARBS til ISERV`() {
        oppfolgingStatusArbs()
        kanReaktiveres()
        val melding = meldingFraArena(Formidlingsgruppe.ISERV, Kvalifiseringsgruppe.BATT)

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(melding)

        verify(kandidatForUtmeldingService, never()).handterUtmeldingsHendelse(any(), any())
    }

    val ISERV_FRA_DATO = LocalDate.now()
    private fun meldingFraArena(formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe): EndringPaaOppfolgingsBruker {
        return EndringPaaOppfolgingsBruker(
            AKTOR_ID,
            FNR.get(),
            formidlingsgruppe = formidlingsgruppe,
            iservFraDato = if (formidlingsgruppe === Formidlingsgruppe.ISERV) ISERV_FRA_DATO else null,
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
        if (underOppfolging) {
            `when`(oppfolgingService.hentGjeldendeOppfolgingsperiode(AKTOR_ID)).thenReturn(
                Optional.of(
                    OppfolgingsperiodeEntity(
                        UUID.randomUUID(),
                        AKTOR_ID.get(),
                        null,
                        ZonedDateTime.now().minusDays(5),
                        null,
                        null,
                        emptyList(),
                        null,
                        "defaultVeileder",
                        StartetAvType.VEILEDER,
                        null
                    )
                )
            )
        }
    }

    private fun oppfolgingStatusArbs() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(
                TestUtils.oppfølgingEntity(aktorId = AKTOR_ID.get(), underOppfolging = true, localArenaOppfølging =
                    LocalArenaOppfolging(
                        Hovedmaal.BEHOLDEA,
                        Kvalifiseringsgruppe.BATT,
                        Formidlingsgruppe.ARBS,
                        null,
                    )
                )
            )
        )
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