package no.nav.veilarboppfolging.service

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2.Companion.builder
import no.nav.veilarboppfolging.LocalDatabaseSingleton.jdbcTemplate
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.*
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.utmelding.IservTrigger
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import no.nav.veilarboppfolging.test.DbTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.ZonedDateTime

class IservServiceIntegrationTest {
    private val iservFraDato: ZonedDateTime = ZonedDateTime.now()
    private var utmeldingsService: UtmeldingsService? = null
    private var utmeldEtter28Cron: UtmeldEtter28Cron? = null
    private var utmeldingRepository: UtmeldingRepository? = null
    private val authService: AuthService = mock(AuthService::class.java)
    private val oppfolgingService: OppfolgingService = mock(OppfolgingService::class.java)
    private val avsluttOppfolgingService: AvsluttOppfolgingService = mock(AvsluttOppfolgingService::class.java)
    private val kandidatForUtmeldingService: KandidatForUtmeldingService = mock(KandidatForUtmeldingService::class.java)

    private val kunneAvsluttesInput = KanAvsluttesInput(
        erUnderOppfolging = false,
        erIservIArena = true,
        harAktiveTiltaksdeltakelser = false,
        erDeltakerIUngdomsprogrammet = false,
        erArbeidssoeker = false,
        harAap = false,
        underKvp = false,
        erOppfolgingForlenget = false
    )

    @BeforeEach
    fun setup() {
        val db = jdbcTemplate

        DbTestUtils.cleanupTestDb()

        whenever(oppfolgingService.erUnderOppfolging(any<AktorId>())).thenReturn(true)
        `when`<KunneAvsluttesResultat>(avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(any<Avregistrering>()))
            .thenReturn(KunneAvsluttes(UtmeldtEtter28Dager(AKTOR_ID), true, kunneAvsluttesInput))
        `when`<Fnr>(authService.getFnrOrThrow(any<AktorId>())).thenReturn(FNR)

        utmeldingRepository = UtmeldingRepository(db)
        utmeldingsService = UtmeldingsService(
            mock<MetricsService?>(MetricsService::class.java)!!,
            utmeldingRepository!!,
            oppfolgingService,
            avsluttOppfolgingService,
            mock()
        )
        utmeldEtter28Cron = UtmeldEtter28Cron(
            utmeldingsService!!,
            utmeldingRepository!!,
            mock<LeaderElectionClient?>(LeaderElectionClient::class.java)!!,
            kandidatForUtmeldingService,
            false
        )
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalLagreNyIservBruker() {
        `when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        val brukerV2 = kanskjeIservBruker()
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        val kanskjeUtmelding = utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID)
        assertTrue(kanskjeUtmelding.isPresent)
        val utmelding = kanskjeUtmelding.get()
        assertEquals(AKTOR_ID.get(), utmelding.aktorId)
        assertEquals(iservFraDato.toLocalDate(), utmelding.iservSiden.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalOppdatereEksisterendeIservBruker() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        val brukerV2 = kanskjeIservBruker(iservFraDato.plusDays(2), Formidlingsgruppe.ISERV)
        utmeldingRepository!!.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isPresent)
        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        val kanskjeUtmelding = utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID)
        assertTrue(kanskjeUtmelding.isPresent)
        val utmelding = kanskjeUtmelding.get()
        assertEquals(AKTOR_ID.get(), utmelding.aktorId)
        assertEquals(brukerV2.iservFraDato, utmelding.iservSiden.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalSletteBrukerSomIkkeLengerErIserv() {
        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.ARBS)

        utmeldingRepository!!.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isPresent)

        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        `when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.IARBS)

        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false)

        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        verifyNoInteractions(oppfolgingService)
    }

    @Test
    fun finnBrukereMedIservI28Dager() {
        assertTrue(utmeldingRepository!!.finnBrukereMedIservI28Dager().isEmpty())

        insertIservBruker(AktorId.of("0"), iservFraDato.minusDays(30))
        insertIservBruker(AktorId.of("1"), iservFraDato.minusDays(27))
        insertIservBruker(AktorId.of("2"), iservFraDato.minusDays(15))
        insertIservBruker(AktorId.of("3"), iservFraDato)

        assertEquals(1, utmeldingRepository!!.finnBrukereMedIservI28Dager().size.toLong())
    }

    @Test
    fun avsluttOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato)

        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isPresent)

        utmeldingsService!!.avsluttOppfolgingOgFjernFraUtmeldingsTabell(AKTOR_ID)

        verify<AvsluttOppfolgingService?>(avsluttOppfolgingService)!!.avsluttOppfolgingHvisKanAvsluttes(
            UtmeldtEtter28Dager(
                AKTOR_ID
            )
        )
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun automatiskAvslutteOppfolging_skal_beholde_bruker_i_utmelding_hvis_behandling_feilet() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))
        val feiledneAKtorId = AktorId.of("404")
        insertIservBruker(feiledneAKtorId, iservFraDato.minusDays(30))
        Mockito.`when`<Boolean?>(oppfolgingService.erUnderOppfolging(feiledneAKtorId))
            .thenThrow(RuntimeException::class.java)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty())
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(feiledneAKtorId).isPresent())
    }

    @Test
    fun automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))
        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        verify<AvsluttOppfolgingService?>(avsluttOppfolgingService, Mockito.never())!!
            .avsluttOppfolgingHvisKanAvsluttes(any<Avregistrering>())
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(29))

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun `skal melde ut kandidat hvis toggle er av og bruker var kandidat for utmelding`() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(29))
        `when`(kandidatForUtmeldingService.erUtmeldingskandidat(AKTOR_ID)).thenReturn(true)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        verify(avsluttOppfolgingService, times(1))
            .avsluttOppfolgingHvisKanAvsluttes(any<Avregistrering>())
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun `skal ikke melde ut kandidat hvis toggle er på og bruker var kandidat for utmelding`() {
        utmeldEtter28Cron = UtmeldEtter28Cron(
            utmeldingsService!!,
            utmeldingRepository!!,
            mock<LeaderElectionClient?>(LeaderElectionClient::class.java)!!,
            kandidatForUtmeldingService,
            true
        )
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(29))
        `when`(kandidatForUtmeldingService.erUtmeldingskandidat(AKTOR_ID)).thenReturn(true)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        verify(avsluttOppfolgingService,never())
            .avsluttOppfolgingHvisKanAvsluttes(any<Avregistrering>())
        assertTrue(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    private fun kanskjeIservBruker(
        iservFraDato: ZonedDateTime = this.iservFraDato,
        formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ISERV
    ): KanskjeIservBruker {
        return KanskjeIservBruker(
            iservFraDato.toLocalDate(),
            AKTOR_ID,
            formidlingsgruppe,
            IservTrigger.OppdateringPaaOppfolgingsBruker,
            true
        )
    }

    private fun insertIservBruker(aktorId: AktorId, iservFraDato: ZonedDateTime): EndringPaaOppfoelgingsBrukerV2 {
        val brukerV2 = builder()
            .fodselsnummer((nesteFnr++).toString())
            .formidlingsgruppe(Formidlingsgruppe.ISERV)
            .iservFraDato(iservFraDato.toLocalDate())
            .build()

        utmeldingRepository!!.insertUtmeldingTabell(OppdateringFraArena_BleIserv(aktorId, iservFraDato))

        return brukerV2
    }

    companion object {
        private val FNR: Fnr = Fnr.of("879037942")
        private val AKTOR_ID: AktorId = AktorId.of("1234")
        private var nesteFnr = 0
    }
}
