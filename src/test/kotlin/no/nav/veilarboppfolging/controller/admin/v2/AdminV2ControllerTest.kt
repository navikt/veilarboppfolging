package no.nav.veilarboppfolging.controller.admin.v2

import java.util.Optional
import java.util.concurrent.TimeUnit
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.veilarboppfolging.controller.admin.v1.POAO_ADMIN
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.KafkaRepubliseringService
import no.nav.veilarboppfolging.test.TestUtils
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(controllers = [AdminV2Controller::class])
class AdminV2ControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authContextHolder: AuthContextHolder

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var kafkaRepubliseringService: KafkaRepubliseringService

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_user_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.empty())
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_role_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvpto-admin"))
        `when`(authContextHolder.role).thenReturn(Optional.empty())

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_not_pto_admin() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvmyapp"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_403_if_not_system_user() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvpto-admin"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.EKSTERN))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserOppfolgingsperioder__should_return_job_id_and_republish() {
        `when`(authContextHolder.subject).thenReturn(Optional.of(POAO_ADMIN))
        `when`(authService.erInternBruker()).thenReturn(true)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/oppfolgingsperioder"))
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.matchesPattern("^([a-f0-9]+)$")))

        TestUtils.verifiserAsynkront(3, TimeUnit.SECONDS) {
            verify(kafkaRepubliseringService, times(1)).republiserOppfolgingsperioder()
        }
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_user_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.empty())
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_role_missing() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvpto-admin"))
        `when`(authContextHolder.role).thenReturn(Optional.empty())

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_not_pto_admin() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvmyapp"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.SYSTEM))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_403_if_not_system_user() {
        `when`(authContextHolder.subject).thenReturn(Optional.of("srvpto-admin"))
        `when`(authContextHolder.role).thenReturn(Optional.of(UserRole.EKSTERN))

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(403))
    }

    @Test
    fun republiserTilordnetVeileder__should_return_job_id_and_republish() {
        `when`(authContextHolder.subject).thenReturn(Optional.of(POAO_ADMIN))
        `when`(authService.erInternBruker()).thenReturn(true)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/admin/republiser/tilordnet-veileder"))
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.matchesPattern("^([a-f0-9]+)$")))

        TestUtils.verifiserAsynkront(3, TimeUnit.SECONDS) {
            verify(kafkaRepubliseringService, times(1)).republiserTilordnetVeileder()
        }
    }
}