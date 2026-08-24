package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.Fnr
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import java.time.LocalDate
import java.time.ZonedDateTime

data class ForlengelseDTO(
    val fnr: Fnr,
    val forlengetTil: LocalDate
)

@RestController
@RequestMapping("/api/forlengelse")
class KandidatForUtmeldingController(
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
) {

    @PostMapping
    fun opprettForlengelse(@RequestBody forlengelseDTO: ForlengelseDTO ) {
        val forlengelseHendelse = ForlengelseHendelse(
            oppfolgingsperiodeUuid =  ,
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = ,
            kilde = "veilarboppfolging",
            forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            forlengetTil = forlengelseDTO.forlengetTil,
        )
        kandidatForUtmeldingService.forlengKandidat()
    }

}