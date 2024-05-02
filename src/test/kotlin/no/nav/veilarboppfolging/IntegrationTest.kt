package no.nav.veilarboppfolging

import com.nimbusds.jwt.JWTClaimsSet

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient
import no.nav.veilarboppfolging.config.ApplicationTestConfig
import no.nav.veilarboppfolging.config.EnvironmentProperties
import no.nav.veilarboppfolging.controller.OppfolgingController
import no.nav.veilarboppfolging.controller.SakController
import no.nav.veilarboppfolging.controller.SystemOppfolgingController
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.SakRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.MetricsService
import no.nav.veilarboppfolging.test.DbTestUtils
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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
    lateinit var azureAdOnBehalfOfTokenClient: AzureAdOnBehalfOfTokenClient

    @MockBean
    lateinit var environmentProperties: EnvironmentProperties

    @MockBean
    lateinit var metricsService: MetricsService

    @MockBean
    lateinit var behandleArbeidssokerClient: BehandleArbeidssokerClient

    @Autowired
    lateinit var aktorOppslagClient: AktorOppslagClient

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var oppfolgingController: OppfolgingController

    @Autowired
    lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository

    @Autowired
    lateinit var sakController: SakController

    @Autowired
    lateinit var systemOppfolgingController: SystemOppfolgingController

    @MockBean
    lateinit var machineToMachineTokenClient: MachineToMachineTokenClient

    @Autowired
    lateinit var sakRepository: SakRepository

    @BeforeEach
    fun beforeEach() {
        DbTestUtils.cleanupTestDb()
    }

    fun startOppfolging(fnr: Fnr): List<OppfolgingPeriodeDTO> {
        val aktiverArbeidssokerData = AktiverArbeidssokerData(
            AktiverArbeidssokerData.Fnr(fnr.get()),
            Innsatsgruppe.STANDARD_INNSATS
        )
        systemOppfolgingController.aktiverBruker(aktiverArbeidssokerData)
        return oppfolgingController.hentOppfolgingsperioder(fnr)
    }

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
}