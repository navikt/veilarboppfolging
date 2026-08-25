package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
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
    val authService: AuthService,
    val oppfolgingService: OppfolgingService,
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository
) {

    @PostMapping
    fun opprettForlengelse(@RequestBody forlengelseDTO: ForlengelseDTO) {
        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)

        if (authService.erInternBruker()) {
            authService.sjekkSkriveTilgangMedFnr(fnr)
            oppfolging?.oppfolgingsEnhet
                ?.let { enhet -> authService.sjekkTilgangTilEnhet(enhet.get()) }
            secureLog.info(
                "Veileder: {} forsøker å forlenge oppfølging for fnr: {}",
                authService.innloggetBrukerIdent,
                fnr.get()
            )
        }
        else if (authService.erEksternBruker()) {
            throw IllegalStateException("Vi støtter ikke at eksternbrukere kan forlenge oppfølging")
        } else {
            secureLog.info("Forsøker å forlenge oppfølging for fnr: {} som systembruker", fnr.get())
        }
        val forlengelseHendelse = ForlengelseHendelse(
            oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(forlengelseDTO.fnr)
                .orElse(null)?.uuid ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ingen gjeldende oppfølgingsperiode funnet for bruker"
            ),
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = authService.innloggetVeilederIdent,
            kilde = "veilarboppfolging",
            forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            forlengetTil = forlengelseDTO.forlengetTil,
        )
        kandidatForUtmeldingService.forlengKandidat(forlengelseHendelse)
    }
}