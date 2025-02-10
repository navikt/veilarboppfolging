package no.nav.veilarboppfolging.oppfolgingsbruker

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Slf4j
@Service
class AktiverBrukerService(
    private val authService: AuthService,
    private val oppfolgingService: OppfolgingService,
    private val transactor: TransactionTemplate
) {

    fun aktiverBrukerManuelt(fnr: Fnr) {
        transactor.executeWithoutResult {
            val aktorId = authService.getAktorIdOrThrow(fnr)
            val navIdent = NavIdent.of(authService.innloggetVeilederIdent)
            val oppfolgingsbruker = Oppfolgingsbruker.manuelRegistrertBruker(aktorId, navIdent)
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker, navIdent.get())
        }
    }
}

