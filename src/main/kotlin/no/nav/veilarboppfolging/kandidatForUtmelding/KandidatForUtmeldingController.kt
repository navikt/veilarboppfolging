package no.nav.veilarboppfolging.kandidatForUtmelding

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forlengelse")
class KandidatForUtmeldingController(
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
) {

    @PostMapping
    fun opprettForlengelse() {
        kandidatForUtmeldingService.forlengKandidat()
    }

}