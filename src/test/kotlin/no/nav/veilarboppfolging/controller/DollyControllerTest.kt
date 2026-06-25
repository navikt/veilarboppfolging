package no.nav.veilarboppfolging.controller

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [DollyController::class])
@org.springframework.test.context.TestPropertySource(properties = ["nais.cluster.name=dev-gcp"])
class DollyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var startOppfolgingService: StartOppfolgingService

    @MockitoBean
    private lateinit var authorizationInterceptor: AuthorizationInterceptor

    private val FNR = Fnr.of("12345678901")
    private val AKTOR_ID = AktorId.of("9876543210")

    @BeforeEach
    fun setup() {
        `when`(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true)
        `when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
    }

    @Test
    fun `gyldig dolly systemtoken gir 200 og starter oppfølging`() {
        doNothing().`when`(authService).skalVereSystemBruker()
        doNothing().`when`(authService).sjekkAtApplikasjonErIAllowList(any<List<String>>())
        doNothing().`when`(startOppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(any())

        mockMvc.perform(
            post("/api/v1/dolly/startOppfolgingsperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(DollyStartOppfolgingRequest(FNR)))
        ).andExpect(status().isOk)

        verify(startOppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(any())
    }

    @Test
    fun `ikke-systembruker (veileder) gir 403`() {
        doThrow(ForbiddenException("Ikke systembruker")).`when`(authService).skalVereSystemBruker()

        mockMvc.perform(
            post("/api/v1/dolly/startOppfolgingsperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(DollyStartOppfolgingRequest(FNR)))
        ).andExpect(status().isForbidden)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
    }

    @Test
    fun `ikke-tillatt applikasjon gir 403`() {
        doNothing().`when`(authService).skalVereSystemBruker()
        doThrow(ForbiddenException("Ikke i allowlist")).`when`(authService).sjekkAtApplikasjonErIAllowList(any<List<String>>())

        mockMvc.perform(
            post("/api/v1/dolly/startOppfolgingsperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(DollyStartOppfolgingRequest(FNR)))
        ).andExpect(status().isForbidden)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
    }

    @Test
    fun `allerede under oppfølging gir 200 (idempotent)`() {
        doNothing().`when`(authService).skalVereSystemBruker()
        doNothing().`when`(authService).sjekkAtApplikasjonErIAllowList(any<List<String>>())
        // startOppfolgingHvisIkkeAlleredeStartet er idempotent og gjør ingenting hvis allerede startet
        doNothing().`when`(startOppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(any())

        mockMvc.perform(
            post("/api/v1/dolly/startOppfolgingsperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(DollyStartOppfolgingRequest(FNR)))
        ).andExpect(status().isOk)
    }
}
