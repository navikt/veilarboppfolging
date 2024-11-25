package no.nav.veilarboppfolging.controller

import lombok.RequiredArgsConstructor
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.BadRequestException
import no.nav.veilarboppfolging.controller.response.*
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response
import no.nav.veilarboppfolging.controller.v3.request.*
import no.nav.veilarboppfolging.oppfolgingsbruker.AktiverBrukerService
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import no.nav.veilarboppfolging.service.*
import no.nav.veilarboppfolging.utils.DtoMappers
import no.nav.veilarboppfolging.utils.auth.AllowListApplicationName
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
class OppfolgingV3Controller(
    val oppfolgingService: OppfolgingService,
    val authService: AuthService,
    val manuellStatusService: ManuellStatusService,
    val kvpService: KvpService,
    val aktiverBrukerService: AktiverBrukerService,
) {

    @PostMapping("/hent-oppfolging")
    fun underOppfolging(@RequestBody oppfolgingRequest: OppfolgingRequest): UnderOppfolgingV2Response {
        val allowlist = listOf<String>(
            AllowListApplicationName.VEILARBVEDTAKSSTOTTE,
            AllowListApplicationName.VEILARBDIALOG,
            AllowListApplicationName.VEILARBAKTIVITET,
            AllowListApplicationName.VEILARBREGISTRERING,
            AllowListApplicationName.VEILARBPORTEFOLJE
        )
        authService.authorizeRequest(oppfolgingRequest.fnr, allowlist)
        return UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(oppfolgingRequest.fnr))
    }

    @GetMapping("/oppfolging/me")
    fun hentBrukerInfo(): Bruker? {
        return Bruker()
            .setId(authService.innloggetBrukerIdent)
            .setErVeileder(authService.erInternBruker())
            .setErBruker(authService.erEksternBruker())
    }

    @PostMapping("/oppfolging/hent-status")
    fun hentOppfolgingsStatus(@RequestBody(required = false) oppfolgingRequest: OppfolgingRequest?): OppfolgingStatus? {
        val maybeFodselsnummer = oppfolgingRequest?.fnr
        val fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer)
        return DtoMappers.tilDto(
            oppfolgingService.hentOppfolgingsStatus(fodselsnummer),
            authService.erInternBruker()
        )
    }

    @PostMapping("/oppfolging/hent-avslutning-status")
    fun hentAvslutningStatus(@RequestBody oppfolgingRequest: OppfolgingRequest): AvslutningStatus {
        authService.skalVereInternBruker()
        return DtoMappers.tilDto(oppfolgingService.hentAvslutningStatus(oppfolgingRequest.fnr))
    }

    @PostMapping("/oppfolging/hent-gjeldende-periode")
    fun hentGjeldendePeriode(@RequestBody oppfolgingRequest: OppfolgingRequest): ResponseEntity<OppfolgingPeriodeMinimalDTO> {
        val allowlist = listOf(
            AllowListApplicationName.VEILARBVEDTAKSSTOTTE,
            AllowListApplicationName.VEILARBDIALOG,
            AllowListApplicationName.VEILARBAKTIVITET,
            AllowListApplicationName.MULIGHETSROMMET
        )
        authService.authorizeRequest(oppfolgingRequest.fnr, allowlist)
        return oppfolgingService.hentGjeldendeOppfolgingsperiode(oppfolgingRequest.fnr)
            .map { oppfolgingsperiode -> DtoMappers.tilOppfolgingPeriodeMinimalDTO(oppfolgingsperiode) }
            .map { op -> ResponseEntity(op, HttpStatus.OK) }
            .orElse(ResponseEntity(HttpStatus.NO_CONTENT))
    }

    @PostMapping(value = ["/oppfolging/hent-perioder"])
    fun hentOppfolgingsperioder(@RequestBody oppfolgingRequest: OppfolgingRequest): List<OppfolgingPeriodeDTO> {
        val allowlist = listOf(AllowListApplicationName.VEILARBVEDTAKSSTOTTE, AllowListApplicationName.AMT_PERSON_SERVICE)
        authService.authorizeRequest(oppfolgingRequest.fnr, allowlist)
        val aktorId = authService.getAktorIdOrThrow(oppfolgingRequest.fnr)
        return hentOppfolgingsperioder(aktorId)
    }

    @PostMapping("/oppfolging/settManuell")
    fun settTilManuell(@RequestBody veilederBegrunnelseRequest: VeilederBegrunnelseRequest): ResponseEntity<*> {
        authService.skalVereInternBruker()

        manuellStatusService.oppdaterManuellStatus(
            veilederBegrunnelseRequest.fnr, true, veilederBegrunnelseRequest.begrunnelse,
            KodeverkBruker.NAV, authService.getInnloggetVeilederIdent()
        )

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Any?>()
    }

    @PostMapping("/oppfolging/settDigital")
    fun settTilDigital(@RequestBody(required = false) veilederBegrunnelseRequest: VeilederBegrunnelseRequest?): ResponseEntity<*> {
        val maybeFodselsnummer = veilederBegrunnelseRequest?.fnr
        val fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer)

        if (authService.erEksternBruker()) {
            manuellStatusService.settDigitalBruker(fodselsnummer)
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Any>()
        }

        // Påkrevd for intern bruker
        if (veilederBegrunnelseRequest == null) {
            throw BadRequestException("veilederBegrunnelseRequest er påkrevd for interne brukere")
        }

        val brukerInfo = hentBrukerInfo()
        if (brukerInfo != null) {
            manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, veilederBegrunnelseRequest.begrunnelse,
                KodeverkBruker.NAV, brukerInfo.getId()
            )
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Any>()
    }

    @PostMapping("/oppfolging/startKvp")
    fun startKvp(@RequestBody startKvp: KvpRequest): ResponseEntity<*> {
        authService.skalVereInternBruker()
        kvpService.startKvp(startKvp.fnr, startKvp.begrunnelse)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Any?>()
    }

    @PostMapping("/oppfolging/stoppKvp")
    fun stoppKvp(@RequestBody stoppKvp: KvpRequest): ResponseEntity<*> {
        authService.skalVereInternBruker()
        kvpService.stopKvp(stoppKvp.fnr, stoppKvp.begrunnelse)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Any?>()
    }

    @PostMapping("/oppfolging/hent-veilederTilgang")
    fun hentVeilederTilgang(@RequestBody veilederRequest: VeilederRequest): VeilederTilgang? {
        authService.skalVereInternBruker()
        return oppfolgingService.hentVeilederTilgang(veilederRequest.fnr)
    }

    @PostMapping("/oppfolging/harFlereAktorIderMedOppfolging")
    fun harFlereAktorIderMedOppfolging(@RequestBody(required = false) oppfolgingRequest: OppfolgingRequest?): Boolean {
        val maybeFodselsnummer = oppfolgingRequest?.fnr
        val fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer)
        return oppfolgingService.hentHarFlereAktorIderMedOppfolging(fodselsnummer)
    }

    @PostMapping("/oppfolging/startOppfolgingsperiode")
    fun aktiverBruker(@RequestBody startOppfolging: StartOppfolgingDto): ResponseEntity<*> {
        authService.skalVereSystemBrukerFraAzureAd()
        authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST)

        aktiverBrukerService.aktiverBrukerManuelt(startOppfolging.fnr)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
    }

    private fun hentOppfolgingsperioder(aktorId: AktorId): List<OppfolgingPeriodeDTO> {
        return oppfolgingService.hentOppfolgingsperioder(aktorId)
            .map { periode -> this.filtrerKvpPerioder(periode) }
            .map { oppfolgingsperiode -> this.mapTilDto(oppfolgingsperiode) }
    }

    private fun mapTilDto(oppfolgingsperiode: OppfolgingsperiodeEntity): OppfolgingPeriodeDTO {
        return DtoMappers.tilOppfolgingPeriodeDTO(oppfolgingsperiode, !authService.erEksternBruker())
    }

    private fun filtrerKvpPerioder(periode: OppfolgingsperiodeEntity): OppfolgingsperiodeEntity {
        if (!authService.erInternBruker() || periode.kvpPerioder == null || periode.kvpPerioder.isEmpty()) {
            return periode
        }

        val kvpPeriodeEntities = periode
            .kvpPerioder
            .filter { it -> authService.harTilgangTilEnhet(it.enhet) }

        return periode.toBuilder().kvpPerioder(kvpPeriodeEntities).build()
    }

    companion object {
        private val ALLOWLIST = listOf(AllowListApplicationName.VEILARBREGISTRERING)
    }
}

class StartOppfolgingDto(
    val fnr: Fnr,
    val henviserSystem: HenviserSystem,
)

enum class HenviserSystem {
    DEMO,
    SYFO,
    AAP
}
