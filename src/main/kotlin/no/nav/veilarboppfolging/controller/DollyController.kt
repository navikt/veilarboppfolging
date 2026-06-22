package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import no.nav.veilarboppfolging.utils.auth.AllowListApplicationName
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endepunkt dedikert til testdatafabrikanten Dolly.
 *
 * Kun tilgjengelig for applikasjoner i [ALLOWLIST] via maskin-til-maskin-token (system-til-system).
 *
 * **NB:** Folkeregisterstatus sjekkes ikke (ingen kall til PDL). Det innebærer at oppfølging
 * kan startes for testpersoner som er registrert som døde, svært unge eller uten lovlig opphold.
 * Dette aksepteres for testformål.
 */
@RestController
@RequestMapping("/api/v1/dolly")
class DollyController(
    private val authService: AuthService,
    private val startOppfolgingService: StartOppfolgingService,
) {

    @PostMapping("/startOppfolgingsperiode")
    fun startOppfolgingsperiode(@RequestBody request: DollyStartOppfolgingRequest) {
        authService.skalVereSystemBruker()
        authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST)

        val fnr = request.fnr
        val aktorId = authService.getAktorIdOrThrow(fnr)
        val oppfolgingsbruker = OppfolgingsRegistrering.dollyRegistrering(fnr, aktorId)
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker)
    }

    companion object {
        private val ALLOWLIST = listOf(
            AllowListApplicationName.DOLLY,
            AllowListApplicationName.DOLLY_PROXY_TRYGDEETATEN,
        )
    }
}

data class DollyStartOppfolgingRequest(val fnr: Fnr)
