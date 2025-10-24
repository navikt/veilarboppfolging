package no.nav.veilarboppfolging.service

import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.BadRequestException
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.kafka.KvpPeriode
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.repository.KvpRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.Optional
import java.util.function.Consumer

@RunWith(MockitoJUnitRunner::class)
class KvpServiceTest{

    @Mock
    private lateinit var kvpRepositoryMock: KvpRepository
    @Mock
    private lateinit var authService: AuthService
    @Mock
    private lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository
    @Mock
    private lateinit var arenaOppfolgingService: ArenaOppfolgingService
    @Mock
    private lateinit var metricsService: MetricsService
    @Mock
    private lateinit var kafkaProducerService: KafkaProducerService
    @Mock
    private lateinit var transactor: TransactionTemplate

    @InjectMocks
    private val kvpService: KvpService? = null

    @Before
    fun initialize() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(true))
        )

        val veilarbArenaOppfolgingsBruker = VeilarbArenaOppfolgingsBruker()
        veilarbArenaOppfolgingsBruker.setNav_kontor(ENHET)
        `when`<EnhetId?>(arenaOppfolgingService.hentArenaOppfolgingsEnhetId(FNR))
            .thenReturn(EnhetId.of(ENHET))

        `when`(authService.harTilgangTilEnhet(ArgumentMatchers.anyString())).thenReturn(true)
        `when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        `when`(authService.getInnloggetVeilederIdent()).thenReturn(VEILEDER)
        doAnswer { mock ->
            val consumer = mock.getArgument<Consumer<Int?>>(0)
            consumer.accept(null)
            null
        }.`when`(transactor)
            .executeWithoutResult(any())
    }

    @Test
    fun start_kvp_uten_oppfolging_er_ulovlig_handling() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(false))
        )

        try {
            kvpService!!.startKvp(FNR, START_BEGRUNNELSE)
        } catch (e: Exception) {
            Assert.assertTrue(e is BadRequestException)
        }
    }

    @Test
    fun start_kvp_uten_bruker_i_oppfolgingtabell_er_ulovlig_handling() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.empty()
        )

        try {
            kvpService!!.startKvp(FNR, START_BEGRUNNELSE)
        } catch (e: Exception) {
            Assert.assertTrue(e is BadRequestException)
        }
    }

    @Test
    fun startKvp() {
        kvpService!!.startKvp(FNR, START_BEGRUNNELSE)
        verify<KvpRepository?>(kvpRepositoryMock, times(1)).startKvp(
            eq(AKTOR_ID),
            eq(ENHET),
            eq(VEILEDER),
            eq(START_BEGRUNNELSE),
            any()
        )
    }

    @Test(expected = BadRequestException::class)
    fun startKvp_feiler_dersom_bruker_allerede_er_under_kvp() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(true).setGjeldendeKvpId(2))
        )

        AuthContextHolderThreadLocal.instance().withContext(
            AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
            UnsafeRunnable { kvpService!!.startKvp(FNR, START_BEGRUNNELSE) }
        )
    }


    fun gittBrukerErUnderOppfolging(kvpId: Long) {
        `when`(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(
            Optional.of(OppfolgingEntity().setUnderOppfolging(true).setGjeldendeKvpId(kvpId))
        )
    }

    fun gittBrukerHarAktivKvp(kvpId: Long, kvpStartTidspunkt: ZonedDateTime, enhetId: String) {
        `when`(kvpRepositoryMock.hentKvpPeriode(kvpId)).thenReturn(
            Optional.of(
                KvpPeriodeEntity.builder()
                    .aktorId(AKTOR_ID.get())
                    .opprettetDato(kvpStartTidspunkt)
                    .build()
            )
        )
        val kvpPeriodeEntity = mock<KvpPeriodeEntity>()
        `when`(kvpPeriodeEntity.enhet).thenReturn(enhetId)
        `when`(kvpRepositoryMock.hentGjeldendeKvpPeriode(AKTOR_ID)).thenReturn(Optional.of(
            kvpPeriodeEntity
        ))
    }

    @Test
    fun stopKvp() {
        val kvpId: Long = 2
        val kvpStartTidspunkt = ZonedDateTime.now().minusDays(7)
        gittBrukerErUnderOppfolging(kvpId)
        gittBrukerHarAktivKvp(kvpId, kvpStartTidspunkt, ENHET)

        AuthContextHolderThreadLocal.instance().withContext(
            AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
            UnsafeRunnable { kvpService!!.stopKvp(FNR, STOP_BEGRUNNELSE) }
        )

        verify(authService, times(1)).sjekkLesetilgangMedAktorId(AKTOR_ID)
        verify(oppfolgingsStatusRepository, times(1)).hentOppfolging(AKTOR_ID)
        verify(kvpRepositoryMock, times(1)).stopKvp(
            eq(kvpId),
            eq(AKTOR_ID),
            eq(VEILEDER),
            eq(STOP_BEGRUNNELSE),
            eq(KodeverkBruker.NAV),
            any()
        )
        verify(authService, times(1)).harTilgangTilEnhet(ENHET)
        verify(kafkaProducerService, times(1)).publiserKvpPeriode(
            KvpPeriode
                .start(AKTOR_ID, ENHET, VEILEDER, kvpStartTidspunkt, START_BEGRUNNELSE)
                .avslutt(VEILEDER, any(), STOP_BEGRUNNELSE)
        )
    }

    @Test
    fun stopKvp_UtenAktivPeriode_feiler() {
        try {
            kvpService!!.stopKvp(FNR, STOP_BEGRUNNELSE)
        } catch (e: Exception) {
            Assert.assertTrue(e is BadRequestException)
        }
    }

    @Test
    fun startKvpInhenEnhetTilgang() {
        `when`<Boolean?>(authService.harTilgangTilEnhet(ENHET)).thenReturn(false)

        try {
            kvpService!!.startKvp(FNR, START_BEGRUNNELSE)
        } catch (e: Exception) {
            Assert.assertTrue(e is ForbiddenException)
        }
    }

    @Test
    fun stopKvpInhenEnhetTilgang() {
        `when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(false)

        try {
            kvpService!!.stopKvp(FNR, STOP_BEGRUNNELSE)
        } catch (e: Exception) {
            Assert.assertTrue(e is ForbiddenException)
        }
    }

    @Test
    fun skal_stoppe_kvp_ved_enhetsbytte() {
        val kvpId: Long = 4
        val kvpStartTidspunkt = ZonedDateTime.now().minusDays(7)
        gittBrukerErUnderOppfolging(kvpId)
        gittBrukerHarAktivKvp(kvpId, kvpStartTidspunkt, ENHET)
        val annenEnhet = "1235"
        val endringPaaOppfolgingsBruker = EndringPaaOppfolgingsBruker(
            AKTOR_ID,
            FNR.get(),
            Formidlingsgruppe.IARBS,
            ZonedDateTime.now(),
            annenEnhet
        )

        kvpService?.avsluttKvpVedEnhetBytte(endringPaaOppfolgingsBruker)

        verify(kvpRepositoryMock, times(1)).stopKvp(
            eq(kvpId),
            eq(AKTOR_ID),
            eq("System"),
            eq("KVP avsluttet automatisk pga. endret Nav-enhet"),
            eq(KodeverkBruker.SYSTEM),
            any()
        )
    }

    companion object {
        private val FNR: Fnr = Fnr.of("1234")
        private val AKTOR_ID: AktorId = AktorId.of("12345")
        private const val ENHET = "1234"
        private const val START_BEGRUNNELSE = "START_BEGRUNNELSE"
        private const val STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE"
        private const val VEILEDER = "1234"
    }
}