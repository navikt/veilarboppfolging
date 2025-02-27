package no.nav.veilarboppfolging.controller.v3

import lombok.RequiredArgsConstructor
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse
import no.nav.veilarboppfolging.controller.v3.request.HistorikkRequest
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.HistorikkService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
class HistorikkV3Controller(
    private val historikkService: HistorikkService,
    private val authService: AuthService
) {
    @PostMapping("/hent-instillingshistorikk")
    fun hentInnstillingsHistorikk(@RequestBody historikkRequest: HistorikkRequest): List<HistorikkHendelse> {
        authService.skalVereInternBruker()
        return historikkService.hentInstillingsHistorikk(historikkRequest.fnr)
    }
}