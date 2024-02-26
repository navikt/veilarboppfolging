package no.nav.veilarboppfolging.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppfolging")
class SakController {

    @GetMapping("/sakid/{oppfolgingsperiodeId}")
    fun hentSakId(@PathVariable oppfolgingsperiodeId: UUID ): Long {
        return -1
    }
}