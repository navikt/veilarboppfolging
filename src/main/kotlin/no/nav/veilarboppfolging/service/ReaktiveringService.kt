package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.client.veilarbarena.*
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException

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

    fun reaktiverBrukerIArena(fnr: Fnr): ReaktiveringResult {
        try {
            return reaktiverBrukerIArenaUtenCatch(fnr)
        } catch (e: Exception) {
            logger.error("Uventet feil ved reaktivering av bruker i Arena", e)
            return UkjentFeilUnderReaktiveringError(
                "Uventet feil ved reaktivering av bruker i Arena: ${e.message}",
                e
            )
        }
    }

    private fun reaktiverBrukerIArenaUtenCatch(fnr: Fnr): ReaktiveringResult {
        val navIdent = NavIdent.of(authService.innloggetVeilederIdent)
        val aktorId = authService.getAktorIdOrThrow(fnr)

        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)
        val erUnderOppfolging = oppfolging?.isUnderOppfolging ?: false

        if (!erUnderOppfolging) return AlleredeUnderoppfolgingError

        val perioder: List<OppfolgingsperiodeEntity> =
            oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
        val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)

        val response = transactor.execute {
            val arenaResponse = arenaOppfolgingService.registrerIkkeArbeidssoker(fnr)
            when (arenaResponse) {
                is RegistrerIArenaSuccess -> {
                    val arenaKode = arenaResponse.arenaResultat.kode
                    when (arenaKode) {
                        in listOf(ArenaRegistreringResultat.FNR_FINNES_IKKE, ArenaRegistreringResultat.KAN_REAKTIVERES_FORENKLET, ArenaRegistreringResultat.UKJENT_FEIL) -> {
                            logger.error("Feil ved registrering av bruker i Arena", arenaResponse.arenaResultat.resultat)
                            return@execute FeilFraArenaError(arenaKode)
                        }
                        else -> {
                            logger.info("Bruker registrert i Arena med resultat: $arenaKode")
                            reaktiveringRepository.insertReaktivering(
                                ReaktiverOppfolgingDto(
                                    aktorId = aktorId,
                                    oppfolgingsperiode = sistePeriode.uuid.toString(),
                                    veilederIdent = navIdent.get(),
                                )
                            )
                            return@execute ReaktiveringSuccess(arenaKode)
                        }
                    }
                }
                is RegistrerIArenaError -> {
                    logger.error("Feil ved registrering av bruker i Arena", arenaResponse.throwable)
                    throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, arenaResponse.message, arenaResponse.throwable
                    )
                }
            }
        } as ReaktiveringResult

        return response
    }
}

data class ReaktiverOppfolgingDto(
    val aktorId: AktorId,
    val oppfolgingsperiode: String,
    val veilederIdent: String,
)