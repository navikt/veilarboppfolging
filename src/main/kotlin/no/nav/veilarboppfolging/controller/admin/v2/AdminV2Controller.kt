package no.nav.veilarboppfolging.controller.admin.v2

import no.nav.common.job.JobRunner
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.controller.admin.v1.POAO_ADMIN
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.KafkaRepubliseringService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/admin")
class AdminV2Controller(
    private val authService: AuthService,
    private val kafkaRepubliseringService: KafkaRepubliseringService,
) {
    @PostMapping("/republiser/oppfolgingsperioder")
    fun republiserOppfolgingsperioder(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-oppfolgingsperioder"
        ) { kafkaRepubliseringService.republiserOppfolgingsperioder() }
    }

    @PostMapping("/republiser/tilordnet-veileder")
    fun republiserTilordnetVeileder(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-tilordnet-veileder"
        ) { kafkaRepubliseringService.republiserTilordnetVeileder() }
    }

    private fun sjekkTilgangTilAdmin() {
        authService.sjekkAtApplikasjonErIAllowList(listOf(POAO_ADMIN))
        if (!authService.erInternBruker()) throw ForbiddenException("Må være internbruker")
    }
}