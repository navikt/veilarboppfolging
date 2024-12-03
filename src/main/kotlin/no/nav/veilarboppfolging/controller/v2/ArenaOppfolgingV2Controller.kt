package no.nav.veilarboppfolging.controller.v2

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.veilarboppfolging.controller.v2.request.ArenaOppfolgingRequest
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusFailure
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusSuccess
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/person")
class ArenaOppfolgingV2Controller(
    val authService: AuthService,
    val arenaOppfolgingService: ArenaOppfolgingService
) {

    /*
     API used by veilarbdetaljerfs. Contains only the necessary information
     */
    @PostMapping("/hent-oppfolgingsstatus")
    fun getOppfolgingsstatus(@RequestBody arenaOppfolgingRequest: ArenaOppfolgingRequest): OppfolgingEnhetMedVeilederResponse {
        authService.sjekkLesetilgangMedFnr(arenaOppfolgingRequest.fnr)
        val result = arenaOppfolgingService.hentArenaOppfolginsstatus(arenaOppfolgingRequest.fnr)
        return when (result) {
            is GetOppfolginsstatusFailure -> throw result.error
            is GetOppfolginsstatusSuccess -> result.result
        }
    }
}
