package no.nav.veilarboppfolging.service

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.domain.AvslutningStatusData
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.UtmeldingsAntall
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldEtter28Cron
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingMetrikkCron
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldtEtter28Dager
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity
import no.nav.veilarboppfolging.service.utmelding.IservTrigger
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import no.nav.veilarboppfolging.test.DbTestUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IservServiceIntegrationTest {
    private val iservFraDato: ZonedDateTime = ZonedDateTime.now()
    private var utmeldingsService: UtmeldingsService? = null
    private var utmeldEtter28Cron: UtmeldEtter28Cron? = null
    private var utmeldingMetrikkCron: UtmeldingMetrikkCron? = null
    private var utmeldingRepository: UtmeldingRepository? = null
    private var oppfolgingsStatusRepository: OppfolgingsStatusRepository? = null
    private val authService: AuthService = Mockito.mock<AuthService>(AuthService::class.java)
    private val oppfolgingService: OppfolgingService = Mockito.mock<OppfolgingService>(OppfolgingService::class.java)
    private val bigQueryMock = Mockito.mock<BigQueryClient>()

    @Before
    fun setup() {
        val db = LocalDatabaseSingleton.jdbcTemplate

        DbTestUtils.cleanupTestDb()

        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(ArgumentMatchers.any<AktorId?>(AktorId::class.java)))
            .thenReturn(true)
        `when`<AvslutningStatusData?>(
            oppfolgingService.avsluttOppfolging(
                ArgumentMatchers.any<Avregistrering?>(
                    Avregistrering::class.java
                )
            )
        ).thenReturn(AvslutningStatusData.builder().underOppfolging(false).build())
        `when`<Fnr?>(authService.getFnrOrThrow(ArgumentMatchers.any<AktorId?>())).thenReturn(FNR)

        oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(db))
        utmeldingRepository = UtmeldingRepository(NamedParameterJdbcTemplate(db))
        utmeldingsService = UtmeldingsService(
            Mockito.mock<MetricsService?>(MetricsService::class.java),
            utmeldingRepository!!,
            oppfolgingService,
            bigQueryMock
        )
        utmeldEtter28Cron = UtmeldEtter28Cron(
            utmeldingsService!!,
            utmeldingRepository!!,
            Mockito.mock<LeaderElectionClient?>(LeaderElectionClient::class.java)
        )
        utmeldingMetrikkCron = UtmeldingMetrikkCron(
            utmeldingRepository!!,
            bigQueryMock
        )
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalLagreNyIservBruker() {
        val bruker = kanskjeIservBruker()
        assertNull(utmeldingRepository!!.eksisterendeIservBruker(bruker.aktorId))
        utmeldingsService!!.oppdaterUtmeldingsStatus(bruker)
        val utmelding = utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID)
        assertNotNull(utmelding)
        assertEquals(AKTOR_ID.get(), utmelding?.aktor_Id)
        assertEquals(iservFraDato.toLocalDate(), utmelding?.iservSiden?.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalOppdatereEksisterendeIservBruker() {
        val brukerV2 = kanskjeIservBruker(iservFraDato.plusDays(2), Formidlingsgruppe.ISERV)
        utmeldingRepository!!.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertNotNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        val utmelding = utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID)
        assertNotNull(utmelding)
        assertEquals(AKTOR_ID.get(), utmelding?.aktor_Id)
        assertEquals(brukerV2.iservFraDato, utmelding?.iservSiden?.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalSletteBrukerSomIkkeLengerErIserv() {
        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.ARBS)

        utmeldingRepository!!.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertNotNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))

        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        assertNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        `when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.IARBS)

        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false)

        utmeldingsService!!.oppdaterUtmeldingsStatus(brukerV2)
        Mockito.verifyNoInteractions(oppfolgingService)
    }

    @Test
    fun finnBrukereMedIservI28Dager() {
        Assert.assertTrue(utmeldingRepository!!.finnBrukereMedIservI28Dager().isEmpty())

        insertIservBruker(AktorId.of("0"), iservFraDato.minusDays(30))
        insertIservBruker(AktorId.of("1"), iservFraDato.minusDays(27))
        insertIservBruker(AktorId.of("2"), iservFraDato.minusDays(15))
        insertIservBruker(AktorId.of("3"), iservFraDato)

        Assert.assertEquals(1, utmeldingRepository!!.finnBrukereMedIservI28Dager().size.toLong())
    }

    @Test
    fun avsluttOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato)

        assertNotNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))

        utmeldingsService!!.avsluttOppfolgingOfFjernFraUtmeldingsTabell(AKTOR_ID)

        verify<OppfolgingService?>(oppfolgingService).avsluttOppfolging(UtmeldtEtter28Dager(AKTOR_ID))
        assertNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
    }

    @Test
    fun automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
    }

    @Test
    fun automatiskAvslutteOppfolging_skal_beholde_bruker_i_utmelding_hvis_behandling_feilet() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))
        val feiledneAKtorId = AktorId.of("404")
        insertIservBruker(feiledneAKtorId, iservFraDato.minusDays(30))
        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(feiledneAKtorId))
            .thenThrow(RuntimeException::class.java)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
        assertNotNull(utmeldingRepository!!.eksisterendeIservBruker(feiledneAKtorId))
    }

    @Test
    fun automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        `when`<Boolean?>(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false)

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        verify<OppfolgingService?>(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any<Avregistrering?>(Avregistrering::class.java))

        assertNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
    }

    @Test
    fun automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet() {
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        `when`<AvslutningStatusData?>(
            oppfolgingService.avsluttOppfolging(
                ArgumentMatchers.any<UtmeldtEtter28Dager?>(
                    UtmeldtEtter28Dager::class.java
                )
            )
        ).thenReturn(AvslutningStatusData.builder().underOppfolging(true).build())

        utmeldEtter28Cron!!.automatiskAvslutteOppfolging()

        assertNotNull(utmeldingRepository!!.eksisterendeIservBruker(AKTOR_ID))
    }

    @Test
    fun `skal telle riktig antall brukere på vei ut av oppfølging`() {
        val brukerIkkeUnderOppfolging = AktorId.of("404")
        val brukerUnderOppfolging = AktorId.of("200")
        insertIservBruker(brukerIkkeUnderOppfolging, iservFraDato.minusDays(30))
        insertIservBruker(brukerUnderOppfolging, iservFraDato.minusDays(30))
        oppfolgingsStatusRepository!!.opprettOppfolging(brukerUnderOppfolging)
        oppfolgingsStatusRepository!!.opprettOppfolging(brukerIkkeUnderOppfolging)
        val db = NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate)
        db.update("UPDATE OPPFOLGINGSTATUS SET UNDER_OPPFOLGING = 0 WHERE AKTOR_ID = :aktorId", mapOf("aktorId" to brukerIkkeUnderOppfolging.get()))
        db.update("UPDATE OPPFOLGINGSTATUS SET UNDER_OPPFOLGING = 1 WHERE AKTOR_ID = :aktorId", mapOf("aktorId" to brukerUnderOppfolging.get()))

        utmeldingMetrikkCron!!.countAntallBrukerePaaVeiUtAvOppfolging()

        verify(bigQueryMock).loggUtmeldingsCount(UtmeldingsAntall(
            personIUtmeldingSomErUnderOppfolging = 1
        ))
    }

    private val arenaBrukerBuilder: EndringPaaOppfoelgingsBrukerV2.EndringPaaOppfoelgingsBrukerV2Builder?
        get() = EndringPaaOppfoelgingsBrukerV2.builder()
            .fodselsnummer(FNR.get())
            .formidlingsgruppe(Formidlingsgruppe.ISERV)
            .iservFraDato(iservFraDato.toLocalDate())

    private fun kanskjeIservBruker(
        iservFraDato: ZonedDateTime = this.iservFraDato,
        formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ISERV
    ): KanskjeIservBruker {
        return KanskjeIservBruker(
            iservFraDato.toLocalDate(),
            AKTOR_ID,
            formidlingsgruppe,
            IservTrigger.OppdateringPaaOppfolgingsBruker
        )
    }

    private fun insertIservBruker(aktorId: AktorId, iservFraDato: ZonedDateTime): EndringPaaOppfoelgingsBrukerV2? {
        val brukerV2 = EndringPaaOppfoelgingsBrukerV2.builder()
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
