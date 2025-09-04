package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Slf4j
@Service
class AktiverBrukerManueltService(
    private val authService: AuthService,
    private val startOppfolgingService: StartOppfolgingService,
    private val transactor: TransactionTemplate
) {

    fun aktiverBrukerManuelt(fnr: Fnr, kontorSattAvVeileder: String?) {
        transactor.executeWithoutResult {
            val aktorId = authService.getAktorIdOrThrow(fnr)
            val navIdent = NavIdent.of(authService.innloggetVeilederIdent)
            val oppfolgingsbruker = OppfolgingsRegistrering.manuellRegistrering(
                fnr, aktorId,
                VeilederRegistrant(navIdent),
                kontorSattAvVeileder
            )
            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker)
        }
    }
}