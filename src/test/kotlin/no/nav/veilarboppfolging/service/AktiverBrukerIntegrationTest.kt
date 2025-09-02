package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient
import no.nav.veilarboppfolging.client.digdir_krr.KRRData
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerManueltService
import no.nav.veilarboppfolging.repository.*
import no.nav.veilarboppfolging.test.DbTestUtils
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class AktiverBrukerIntegrationTest : IntegrationTest() {
//    private lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository
    private var aktiverBrukerManueltService: AktiverBrukerManueltService? = null
    private var manuellStatusService: ManuellStatusService? = null
    private val FNR: Fnr = Fnr.of("1111")
    private val AKTOR_ID: AktorId = AktorId.of("1234523423")
    private var bigQueryClient: BigQueryClient? = null
    private var kafkaProducerService: KafkaProducerService? = null

    @Before
    fun setup() {
//        val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
//        val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
//        val transactor = DbTestUtils.createTransactor(jdbcTemplate)
//        oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(jdbcTemplate))
//        oppfolgingsPeriodeRepository = OppfolgingsPeriodeRepository(jdbcTemplate, transactor)

        authService = Mockito.mock(AuthService::class.java)
        Mockito.`when`(authService.getFnrOrThrow(AKTOR_ID)).thenReturn(FNR)
        bigQueryClient = Mockito.mock(BigQueryClient::class.java)
        kafkaProducerService = Mockito.mock(KafkaProducerService::class.java)
        manuellStatusService = Mockito.mock(ManuellStatusService::class.java)

//        oppfolgingService = OppfolgingService(
//            kafkaProducerService,
//            null,
//            null,
//            authService,
//            oppfolgingsStatusRepository,
//            oppfolgingsPeriodeRepository,
//            manuellStatusService,
//            Mockito.mock(AmtDeltakerClient::class.java),
//            KvpRepository(jdbcTemplate, namedParameterJdbcTemplate, transactor),
//            MaalRepository(jdbcTemplate, transactor),
//            Mockito.mock(BrukerOppslagFlereOppfolgingAktorRepository::class.java),
//            transactor,
//            Mockito.mock(ArenaYtelserService::class.java),
//            bigQueryClient,
//            "https://test.nav.no"
//        )

//        startOppfolgingService = StartOppfolgingService(
//            manuellStatusService!!,
//            oppfolgingsStatusRepository!!,
//            oppfolgingsPeriodeRepository,
//            kafkaProducerService!!,
//            bigQueryClient!!,
//            transactor,
//            Mockito.mock(ArenaOppfolgingService::class.java),
//            "https://test.nav.no"
//        )

//        aktiverBrukerManueltService = AktiverBrukerManueltService(
//            authService,
//            startOppfolgingService,
//            DbTestUtils.createTransactor(jdbcTemplate)
//        )

        DbTestUtils.cleanupTestDb()
        Mockito.`when`(authService.getAktorIdOrThrow(ArgumentMatchers.any<Fnr?>(Fnr::class.java)))
            .thenReturn(AKTOR_ID)
        Mockito.`when`(authService.getInnloggetVeilederIdent()).thenReturn("G321321")
        Mockito.`when`(manuellStatusService!!.hentDigdirKontaktinfo(ArgumentMatchers.any<Fnr?>()))
            .thenReturn(KRRData())
    }

    @Test
    fun skalLagreIDatabaseDersomKallTilArenaErOK() {
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.isPresent()).isTrue()
    }

    @Test
    fun skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository!!.opprettOppfolging(AKTOR_ID)
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse")
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }

    @Test
    fun aktiver_sykmeldt_skal_starte_oppfolging() {
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolgingFør.isEmpty()).isTrue()
        aktiverBrukerManueltService!!.aktiverBrukerManuelt(FNR, "1234")
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }
}
