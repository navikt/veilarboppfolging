package no.nav.veilarboppfolging.controller.admin.v2

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import java.util.UUID
import no.nav.common.job.JobRunner
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.client.aoKontor.AoKontorClient
import no.nav.veilarboppfolging.client.veilarbarena.ArenaRegistreringResultat
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaError
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaSuccess
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIkkeArbeidssokerDto
import no.nav.veilarboppfolging.controller.admin.v1.POAO_ADMIN
import no.nav.veilarboppfolging.controller.response.AvslutningsStatusDto
import no.nav.veilarboppfolging.kandidatForUtmelding.RepubliserKandidatForUtmeldingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerManueltService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaRepubliseringService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import no.nav.veilarboppfolging.utils.DtoMappers
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v2/admin")
class AdminV2Controller(
    private val authService: AuthService,
    private val kafkaRepubliseringService: KafkaRepubliseringService,
    private val republiserKandidatForUtmeldingService: RepubliserKandidatForUtmeldingService,
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val oppfolgingsperiodeService: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val startOppfolgingService: StartOppfolgingService,
    private val aoKontorClient: AoKontorClient,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val aktiverBrukerManueltService: AktiverBrukerManueltService,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/republiser/oppfolgingsperioder")
    fun republiserOppfolgingsperioder(): String {
        sjekkTilgangTilAdmin()
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

    @PostMapping("/republiser/utmeldingskandidat")
    fun republiserUtmeldingskandidat(@RequestBody republiserKandidatForUtmeldingRequest: RepubliserKandidatForUtmeldingRequest): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-utmeldingskandidat"
        ) {
            republiserKandidatForUtmeldingService.republiserKandidatForUtmelding(
                UUID.fromString(
                    republiserKandidatForUtmeldingRequest.oppfolgingsperiodeId
                )
            )
        }
    }

    @PostMapping("/republiser/utmeldingskandidater/aktive")
    fun republiserAktiveUtmeldingskandidater(): String {
        sjekkTilgangTilAdmin()
        return JobRunner.runAsync(
            "republiser-aktive-utmeldingskandidater"
        ) {
            republiserKandidatForUtmeldingService.republiserAlleAktiveUtmeldingskandidater()
        }
    }

    @PostMapping("/avslutning-status")
    fun hentAvslutningStatusForOppfolgingsperioder(
        @RequestBody request: HentAvslutningStatusForOppfolgingsperioderRequest
    ): Map<String, AvslutningsStatusDto?> {
        sjekkTilgangTilAdmin()

        return request.oppfolgingsperiodeIder.associateWith { oppfolgingsperiodeId ->
            oppfolgingsperiodeService
                .hentOppfolgingsperiode(oppfolgingsperiodeId)
                .orElse(null)?.aktorId
                ?.let { aktorOppslagClient.hentFnr(AktorId(it)) }
                ?.let { avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(it) }
                ?.let { DtoMappers.tilDto(it) }
        }
    }

    @PostMapping("/batch/start-oppfolging-med-forrige-kontor")
    fun batchStartOppfolgingsperioder(@RequestBody input: BatchStartOppfolging): List<ResponseEntity<RegistrerIkkeArbeidssokerDto>> {
        val result = input.fnrList.map { fnr ->
            val kontor = aoKontorClient.hentForrigeAoKontor(Fnr.of(fnr))
            val arenaResponse = arenaOppfolgingService.registrerIkkeArbeidssoker(Fnr.of(fnr))
            when (arenaResponse) {
                is RegistrerIArenaSuccess -> {
                    when (arenaResponse.arenaResultat.kode) {
                        ArenaRegistreringResultat.FNR_FINNES_IKKE, ArenaRegistreringResultat.KAN_REAKTIVERES_FORENKLET, ArenaRegistreringResultat.UKJENT_FEIL -> {
                            logger.error(
                                "Feil ved registrering av bruker i Arena: {}",
                                arenaResponse.arenaResultat.resultat
                            )
                            ResponseEntity(arenaResponse.arenaResultat, HttpStatus.CONFLICT)
                        }

                        else -> {
                            logger.info("Bruker registrert i Arena med resultat: ${arenaResponse.arenaResultat.kode}")
                            aktiverBrukerManueltService.aktiverBrukerMedForrigeAoKontor(
                                fnr = Fnr.of(fnr),
                                forrigeAoKontor = kontor,
                            )
                            ResponseEntity(arenaResponse.arenaResultat, HttpStatus.OK)
                        }
                    }
                }

                is RegistrerIArenaError -> {
                    logger.error("Feil ved registrering av bruker i Arena", arenaResponse.throwable)
                    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, arenaResponse.message)
                }
            }
        }
        return result
    }

    private fun sjekkTilgangTilAdmin() {
        authService.sjekkAtApplikasjonErIAllowList(listOf(POAO_ADMIN))
        if (!authService.erInternBruker()) throw ForbiddenException("Må være internbruker")
    }
}

data class HentAvslutningStatusForOppfolgingsperioderRequest(
    val oppfolgingsperiodeIder: List<String>,
)

data class BatchStartOppfolging(
    val fnrList: List<String>,
)