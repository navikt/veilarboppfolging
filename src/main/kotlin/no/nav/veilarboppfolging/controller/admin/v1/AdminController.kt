package no.nav.veilarboppfolging.controller.admin.v1

import kotlin.jvm.optionals.getOrNull
import no.nav.common.job.JobRunner
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.controller.response.Veilarbportefoljeinfo
import no.nav.veilarboppfolging.controller.v2.request.RepubliserVeilederRequest
import no.nav.veilarboppfolging.domain.AvsluttOppfolgingsperiodePayload
import no.nav.veilarboppfolging.domain.RepubliserOppfolgingsperioderRequest
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AdminAvregistrering
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaRepubliseringService
import no.nav.veilarboppfolging.service.ManuellStatusService
import no.nav.veilarboppfolging.service.RepubliserOppfolgingshendelseService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val POAO_ADMIN = "poao-admin"

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val authService: AuthService,
    private val kafkaRepubliseringService: KafkaRepubliseringService,
    private val veilederTilordningerRepository: VeilederTilordningerRepository,
    private val manuellStatusService: ManuellStatusService,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val republiserOppfolgingshendelseService: RepubliserOppfolgingshendelseService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/republiser/oppfolgingsperioder")
    fun republiserOppfolgingsperioder(@RequestBody(required = false) request: RepubliserOppfolgingsperioderRequest?): String {
        sjekkTilgangTilAdmin()

        if (request != null) {
            return JobRunner.runAsync(
                "republiser-oppfolgingsperioder-for-bruker"
            ) { kafkaRepubliseringService.republiserOppfolgingsperiodeForBruker(request.aktorId) }
        }

        return JobRunner.runAsync(
            "republiser-oppfolgingsperioder"
        ) { kafkaRepubliseringService.republiserOppfolgingsperioder() }
    }

    @PostMapping("/republiser/tilordnet-veileder")
    fun republiserTilordnetVeileder(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-tilordnet-veileder"
        ) { kafkaRepubliseringService.republiserTilordnetVeileder() }
    }

    @PostMapping("/republiser/tilordnet-veileder/utvalg")
    fun republiserTilordnetVeileder(@RequestBody republiserVeilederRequest: RepubliserVeilederRequest): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-tilordnet-veileder-gitte-aktorider"
        ) { kafkaRepubliseringService.republiserTilordnetVeileder(republiserVeilederRequest.aktorIder) }
    }

    @PostMapping("/republiser/kvp-perioder")
    fun republiserKvpPerioder(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync("republiser-kvp-perioder") { kafkaRepubliseringService.republiserKvpPerioder() }
    }

    @GetMapping("/hentVeilarbinfo/bruker")
    fun hentVeilarbportefoljeinfo(@RequestParam aktorId: AktorId): Veilarbportefoljeinfo {
        authService.skalVereSystemBruker()
        val tilordningEntity = veilederTilordningerRepository.hentTilordnetVeileder(aktorId).getOrNull()
        val erManuell = manuellStatusService.hentManuellStatus(aktorId).getOrNull()?.manuell ?: false
        val startDato = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).getOrNull()?.startDato

        return Veilarbportefoljeinfo(
            aktorId = aktorId,
            veilederId = tilordningEntity?.veilederId?.let { NavIdent.of(it) },
            erUnderOppfolging = tilordningEntity?.oppfolging ?: false,
            nyForVeileder = tilordningEntity?.nyForVeileder ?: false,
            erManuell = erManuell,
            startDato = startDato,
            tilordnetTidspunkt = tilordningEntity?.sistTilordnet,
        )
    }

    @PostMapping("/avsluttBrukere")
    fun batchAvsluttBrukere(@RequestBody brukereSomSkalAvsluttes: AvsluttPayload): AvsluttResultat {
        sjekkTilgangTilAdmin()
        val innloggetBruker = authService.getInnloggetVeilederIdent()
        log.info("Skal avslutte oppfølging for {} brukere", brukereSomSkalAvsluttes.aktorIds.size)

        val resultat = brukereSomSkalAvsluttes.aktorIds.map { aktorId ->
            runCatching {
                avsluttOppfolgingService.adminAvsluttOppfolgingForBruker(
                    AdminAvregistrering(
                        AktorId.of(aktorId),
                        VeilederRegistrant(NavIdent(innloggetBruker)),
                        brukereSomSkalAvsluttes.begrunnelse,
                        null,
                    ),
                )
            }.onFailure { e ->
                log.warn("Kunne ikke avslutte oppfølging: {}", e.message)
            }.isSuccess
        }

        val avsluttedeBrukere = resultat.count { it }
        val ikkeAvsluttedeBrukere = resultat.count { !it }

        log.info("Avsluttet oppfølging for {} brukere", avsluttedeBrukere)
        log.info("Kunne ikke avslutte oppfølging for {} brukere", ikkeAvsluttedeBrukere)

        return AvsluttResultat(avsluttedeBrukere, ikkeAvsluttedeBrukere)
    }

    @PostMapping("/avsluttOppfolgingsperiode")
    fun avsluttOppfolgingsperiode(@RequestBody oppfolgingsperiodeSomSkalAvsluttes: AvsluttOppfolgingsperiodePayload): Boolean {
        sjekkTilgangTilAdmin()
        val innloggetBruker = authService.getInnloggetVeilederIdent()
        try {
            avsluttOppfolgingService.adminAvsluttSpesifikkOppfolgingsperiode(
                AktorId.of(oppfolgingsperiodeSomSkalAvsluttes.aktorId),
                innloggetBruker,
                oppfolgingsperiodeSomSkalAvsluttes.begrunnelse,
                oppfolgingsperiodeSomSkalAvsluttes.oppfolgingsperiodeUuid
            )
            return true
        } catch (e: Exception) {
            log.warn("Kunne ikke avslutte oppfølgingsperiode: {}", e.message)
            return false
        }
    }

    @PostMapping("/republiser/oppfolgingshendelse")
    fun republiserOppfolgingshendelse(@RequestBody aktorId: AktorId) {
        sjekkTilgangTilAdmin()
        republiserOppfolgingshendelseService.republiserOppfolgingshendelseForBruker(aktorId)
    }

    private fun sjekkTilgangTilAdmin() {
        if (!authService.erInternBruker()) throw ForbiddenException("Må være internbruker")
        authService.sjekkAtApplikasjonErIAllowList(listOf(POAO_ADMIN))
    }
}