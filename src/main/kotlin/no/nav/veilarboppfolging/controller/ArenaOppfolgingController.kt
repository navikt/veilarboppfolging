package no.nav.veilarboppfolging.controller

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusFailure
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusSuccess
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
class ArenaOppfolgingController(
    val authService: AuthService,
    val arenaOppfolgingService: ArenaOppfolgingService
) {

    @GetMapping("/oppfolgingsenhet")
    fun getOppfolgingsenhet(@RequestParam("aktorId") aktorId: AktorId?): OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet {
        authService.sjekkLesetilgangMedAktorId(aktorId)
        val fnr = authService.getFnrOrThrow(aktorId)
        val arenaOppfolgingBrukerResult = arenaOppfolgingService.getOppfolginsstatus(fnr)
        when (arenaOppfolgingBrukerResult) {
            is GetOppfolginsstatusFailure -> throw arenaOppfolgingBrukerResult.error
            is GetOppfolginsstatusSuccess -> return arenaOppfolgingBrukerResult.result.oppfolgingsenhet
        }
    }
}
