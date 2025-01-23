package no.nav.veilarboppfolging

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.config.ApplicationTestConfig
import no.nav.veilarboppfolging.config.EnvironmentProperties
import no.nav.veilarboppfolging.controller.OppfolgingController
import no.nav.veilarboppfolging.controller.SakController
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.Oppfolgingsbruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.SakRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.MetricsService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.test.DbTestUtils
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdOnBehalfOfTokenClient
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.util.*

@EmbeddedKafka(partitions = 1)
@SpringBootTest(classes = [ApplicationTestConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
open class IntegrationTest {

    @MockBean
    lateinit var authContextHolder: AuthContextHolder

    @MockBean
    lateinit var azureAdOnBehalfOfTokenClient: ErrorMappedAzureAdOnBehalfOfTokenClient

    @MockBean
    lateinit var norg2Client: Norg2Client

    @MockBean
    lateinit var environmentProperties: EnvironmentProperties

    @MockBean
    lateinit var metricsService: MetricsService

    @Autowired
    lateinit var aktorOppslagClient: AktorOppslagClient

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var oppfolgingController: OppfolgingController

    @Autowired
    lateinit var oppfolgingService: OppfolgingService

    @Autowired
    lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository

    @Autowired
    lateinit var sakController: SakController

    @Autowired
    lateinit var arenaOppfolgingService: ArenaOppfolgingService

    @Autowired
    lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository

    @MockBean
    lateinit var azureMachineToMachineTokenClient: ErrorMappedAzureAdMachineToMachineTokenClient

    @Autowired
    lateinit var sakRepository: SakRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var enhetRepository: EnhetRepository

    @BeforeEach
    fun beforeEach() {
        DbTestUtils.cleanupTestDb(jdbcTemplate)
    }

    fun startOppfolgingSomArbeidsoker(aktørId: AktorId) {
        val bruker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, StartetAvType.BRUKER)
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(bruker)
    }

    fun hentOppfolgingsperioder(fnr: Fnr) = oppfolgingController.hentOppfolgingsperioder(fnr)

    fun avsluttOppfolging(aktorId: AktorId, veileder: String = "veileder", begrunnelse: String = "Begrunnelse") {
        oppfolgingsPeriodeRepository.avslutt(aktorId, veileder, begrunnelse )
    }

    fun mockAuthOk(aktørId: AktorId, fnr: Fnr) {
        val claims = JWTClaimsSet.Builder()
            .issuer("microsoftonline.com")
            .claim("azp_name", "cluster:team:veilarbregistrering")
            .claim("roles", listOf("access_as_application"))
            .build()

        Mockito.`when`(authContextHolder.idTokenClaims).thenReturn(Optional.of(claims))

        val token = "token"

        Mockito.`when`(authContextHolder.idTokenString).thenReturn(Optional.of(token))

        Mockito.`when`(authContextHolder.erSystemBruker()).thenReturn(true)
        Mockito.`when`(aktorOppslagClient.hentAktorId(fnr))
            .thenReturn(aktørId)
        Mockito.`when`(aktorOppslagClient.hentFnr(aktørId))
            .thenReturn(fnr)
    }

    fun mockInternBrukerAuthOk(veilederIOD: UUID,aktørId: AktorId, fnr: Fnr) {
        val claims = JWTClaimsSet.Builder()
            .issuer("microsoftonline.com")
            .claim("azp_name", "cluster:team:veilarbregistrering")
            .claim("oid", veilederIOD.toString())
            .build()

        Mockito.`when`(authContextHolder.idTokenClaims).thenReturn(Optional.of(claims))

        val token = "token"

        Mockito.`when`(authContextHolder.idTokenString).thenReturn(Optional.of(token))

        Mockito.`when`(authContextHolder.erInternBruker()).thenReturn(true)
        Mockito.`when`(aktorOppslagClient.hentAktorId(fnr))
            .thenReturn(aktørId)
        Mockito.`when`(aktorOppslagClient.hentFnr(aktørId))
            .thenReturn(fnr)
    }
}