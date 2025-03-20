package no.nav.veilarboppfolging.controller

import lombok.RequiredArgsConstructor
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.controller.v3.request.HentSisteBesvarelseRequest
import no.nav.veilarboppfolging.domain.StartSamtale
import no.nav.veilarboppfolging.repository.StartSamtaleRepository
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/motestotte")
class StartSamtaleController(
    private val startSamtaleRepository: StartSamtaleRepository,
    private val authService: AuthService
) {

    @PostMapping("/besvarelse")
    fun nyttSvar(@RequestBody hentSisteBesvarelseRequest: HentSisteBesvarelseRequest): ResponseEntity<*> {
        val brukerFnr: Fnr = hentSisteBesvarelseRequest.fnr()
        val aktorId: AktorId = authService.getAktorIdOrThrow(brukerFnr)

        authService.sjekkLesetilgangMedAktorId(aktorId)

        startSamtaleRepository.oppdaterSisteStartSamtaleInnsending(aktorId)

        return ResponseEntity.status(204).build<Any>()
    }

    @PostMapping("/hent-sisteSamtaleInnsending")
    fun hentSisteSamtaleInnsending(@RequestBody hentSisteBesvarelseRequest: HentSisteBesvarelseRequest): StartSamtale {
        val brukerFnr: Fnr = hentSisteBesvarelseRequest.fnr()
        val aktorId: AktorId = authService.getAktorIdOrThrow(brukerFnr)

        authService.sjekkLesetilgangMedAktorId(aktorId)

        val sisteSamtaleInnsending: StartSamtale = startSamtaleRepository.hentSisteStartSamtaleInnsending(aktorId)
            ?: throw ResponseStatusException(HttpStatus.NO_CONTENT)

        return sisteSamtaleInnsending
    }
}