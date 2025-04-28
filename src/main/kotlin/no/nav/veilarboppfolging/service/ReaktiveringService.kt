package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.client.veilarbarena.ARENA_REGISTRERING_RESULTAT
import no.nav.veilarboppfolging.client.veilarbarena.ReaktiveringResponse
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaError
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaSuccess
import no.nav.veilarboppfolging.controller.ReaktiverOppfolgingDto
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.function.Function

@Service
class ReaktiveringService(
    val authService: AuthService,
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    val arenaOppfolgingService: ArenaOppfolgingService,
    val reaktiveringRepository: ReaktiveringRepository,
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val transactor: TransactionTemplate,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReaktiveringService::class.java)

    fun reaktiverBrukerIArena(reaktiverOppfolgingDto: ReaktiverOppfolgingDto): ResponseEntity<ReaktiveringResponse> {

        val navIdent = NavIdent.of(authService.innloggetVeilederIdent)
        val aktorId = authService.getAktorIdOrThrow(reaktiverOppfolgingDto.fnr);

        val maybeOppfolging: Optional<OppfolgingEntity> = oppfolgingsStatusRepository.hentOppfolging(aktorId)

        val erUnderOppfolging =
            maybeOppfolging.map<Boolean>(Function { obj: OppfolgingEntity? -> obj!!.isUnderOppfolging })
                .orElse(false)

        if (!erUnderOppfolging) {
            return ResponseEntity(ReaktiveringResponse(false, "Bruker kan ikke reaktiveres"), HttpStatus.CONFLICT)
        }

        val perioder: MutableList<OppfolgingsperiodeEntity?>? =
            oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
        val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)

        val response = transactor.execute {

            val arenaResponse = arenaOppfolgingService.registrerIkkeArbeidssoker(reaktiverOppfolgingDto.fnr)

            val historikkForReaktiveringDto = HistorikkForReaktiveringDto(
                aktorId = aktorId.toString(),
                oppfolgingsperiode = sistePeriode.uuid.toString(),
                veilederIdent = navIdent.get(),
            )

            when (arenaResponse) {
                is RegistrerIArenaSuccess -> {
                    when (arenaResponse.arenaResultat.kode) {
                        ARENA_REGISTRERING_RESULTAT.FNR_FINNES_IKKE, ARENA_REGISTRERING_RESULTAT.KAN_REAKTIVERES_FORENKLET, ARENA_REGISTRERING_RESULTAT.UKJENT_FEIL -> {
                            logger.error(
                                "Feil ved registrering av bruker i Arena",
                                arenaResponse.arenaResultat.resultat
                            )
                            ResponseEntity(
                                ReaktiveringResponse(false, arenaResponse.arenaResultat.kode.name),
                                HttpStatus.CONFLICT
                            )
                        }

                        else -> {
                            logger.info("Bruker registrert i Arena med resultat: ${arenaResponse.arenaResultat.kode}")
                            reaktiveringRepository.insertReaktivering(historikkForReaktiveringDto)
                            ResponseEntity(
                                ReaktiveringResponse(true, arenaResponse.arenaResultat.kode.name),
                                HttpStatus.OK
                            )
                        }
                    }
                }

                is RegistrerIArenaError -> {
                    logger.error("Feil ved registrering av bruker i Arena", arenaResponse.throwable)
                    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, arenaResponse.message)
                }
            }
        }

        return response
    }
}

data class HistorikkForReaktiveringDto(
    val aktorId: String,
    val oppfolgingsperiode: String,
    val veilederIdent: String,
)