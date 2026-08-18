package no.nav.veilarboppfolging.controller.admin.v1

import java.util.Optional
import java.util.concurrent.TimeUnit
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.domain.RepubliserOppfolgingsperioderRequest
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaRepubliseringService
import no.nav.veilarboppfolging.service.ManuellStatusService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.RepubliserOppfolgingshendelseService
import no.nav.veilarboppfolging.test.TestData
import no.nav.veilarboppfolging.test.TestUtils
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(controllers = [AdminController::class])
class AdminControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authContextHolder: AuthContextHolder

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var kafkaRepubliseringService: KafkaRepubliseringService

    @MockitoBean
    private lateinit var veilederTilordningerRepository: VeilederTilordningerRepository

    @MockitoBean
    private lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository

    @MockitoBean
    private lateinit var manuellStatusService: ManuellStatusService

    @MockitoBean
    private lateinit var oppfolgingService: OppfolgingService

    @MockitoBean
    private lateinit var avsluttOppfolgingService: AvsluttOppfolgingService

    @MockitoBean
    private lateinit var aktorOppslagClient: AktorOppslagClient

    @MockitoBean
    private lateinit var republiserOppfolgingshendelseService: RepubliserOppfolgingshendelseService

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_user_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`<Optional<UserRole>>(authContextHolder.role).thenReturn(Optional.of<UserRole>(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_role_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authService.erSystemBruker()).thenReturn(false)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_not_pto_admin() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvmyapp"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_not_system_user() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authService.erSystemBruker()).thenReturn(false)
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.EKSTERN))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_job_id_and_republish() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))
        `when`(authService.erInternBruker()).thenReturn(true)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.matchesPattern("^([a-f0-9]+)$")))

        TestUtils.verifiserAsynkront(3, TimeUnit.SECONDS) {
            verify(kafkaRepubliseringService, times(1)).republiserOppfolgingsperioder()
        }
    }

    @Test
    fun republiserOppfolgingsperiodeForBruker__should_return_job_id_and_republish() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))
        `when`(authService.erInternBruker()).thenReturn(true)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/admin/republiser/oppfolgingsperioder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(RepubliserOppfolgingsperioderRequest(AktorId.of(TestData.TEST_AKTOR_ID.get()))))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.matchesPattern("^([a-f0-9]+)$")))

        TestUtils.verifiserAsynkront(3, TimeUnit.SECONDS) {
            verify(kafkaRepubliseringService, times(1)).republiserOppfolgingsperiodeForBruker(TestData.TEST_AKTOR_ID)
        }
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_user_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.empty())
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_role_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authContextHolder.role).thenReturn(Optional.empty())

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_not_pto_admin() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvmyapp"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_not_system_user() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.EKSTERN))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_job_id_and_republish() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("sub"))
        `when`(authService.hentApplikasjonFraContext()).thenReturn(POAO_ADMIN)
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))
        `when`(authService.erInternBruker()).thenReturn(true)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.matchesPattern("^([a-f0-9]+)$")))

        TestUtils.verifiserAsynkront(3, TimeUnit.SECONDS) {
            verify(kafkaRepubliseringService, times(1)).republiserTilordnetVeileder()
        }
    }
}