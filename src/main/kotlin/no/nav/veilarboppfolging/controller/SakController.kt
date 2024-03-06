package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.SakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import kotlin.jvm.optionals.getOrElse

@RestController
@RequestMapping("/api/v3/sak")
class SakController(
    private val sakService: SakService,
    private val oppfolgingService: OppfolgingService,
    private val authService: AuthService) {

    @PostMapping("/{oppfolgingsperiodeId}")
    fun opprettEllerHentSak(@PathVariable oppfolgingsperiodeId: UUID ): SakDTO {
        val oppfølgingsperiode = oppfolgingService.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
        authService.sjekkLesetilgangMedAktorId(AktorId.of(oppfølgingsperiode.aktorId))

        return sakService.hentSak(oppfolgingsperiodeId).let { SakDTO(it.oppfølgingsperiodeUUID, it.id) }
    }

    data class SakDTO(
        val oppfolgingsperiodeId: UUID,
        val sakId: Long,
    ) {
        val fagsaksystem: String = "ARBEIDSOPPFOLGING"
    }
}