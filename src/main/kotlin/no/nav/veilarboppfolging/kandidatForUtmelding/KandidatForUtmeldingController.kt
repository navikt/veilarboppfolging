package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(KandidatForUtmeldingController::class.java)

    @PostMapping
    fun opprettForlengelse(@RequestBody forlengelseDTO: ForlengelseDTO) {
        val aktorId = authService.getAktorIdOrThrow(forlengelseDTO.fnr)
        val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(forlengelseDTO.fnr)
            .orElse(null)?.uuid ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ingen gjeldende oppfølgingsperiode funnet for bruker"
        )
        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)

        if (authService.erInternBruker()) {
            authService.sjekkSkriveTilgangMedFnr(forlengelseDTO.fnr)
            oppfolging?.oppfolgingsEnhet
                ?.let { enhet -> authService.sjekkTilgangTilEnhet(enhet.get()) }
            logger.info(
                "Veileder forsøker å forlenge oppfølging for oppfølgingsperiodeId: {}",
                oppfolgingsperiodeId
            )
        } else {
            throw IllegalStateException("Kun veileder kan forlenge oppfølging")
        }

        val forlengelseHendelse = ForlengelseHendelse(
            oppfolgingsperiodeUuid = oppfolgingsperiodeId,
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