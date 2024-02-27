package no.nav.veilarboppfolging.controller

import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.SakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/oppfolging")
class SakController(private val sakService: SakService, private val oppfolgingService: OppfolgingService) {

    @GetMapping("/sakid/{oppfolgingsperiodeId}")
    fun hentSak(@PathVariable oppfolgingsperiodeId: UUID ): SakDTO {
        val erGyldigOppfølgingsperiodeUUID = oppfolgingService.hentOppfolgingsperiode(oppfolgingsperiodeId.toString()).isPresent
        if (!erGyldigOppfølgingsperiodeUUID) throw ResponseStatusException(HttpStatus.BAD_REQUEST);

        return sakService.hentSak(oppfolgingsperiodeId).let { SakDTO(it.oppfølgingsperiodeUUID, it.id) }
    }

    data class SakDTO(
        val oppfolgingsperiodeId: UUID,
        val sakId: Long
    )
}