package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
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
    private val transactor: TransactionTemplate,
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient
) {

    fun aktiverBrukerManuelt(fnr: Fnr, kontorSattAvVeileder: String?) {
        val manueltSjekketLovligOpphold = aktiveringKrevdeManuellSjekkAvLovligOppholdEllerThrow(fnr)

        transactor.executeWithoutResult {
            val aktorId = authService.getAktorIdOrThrow(fnr)
            val navIdent = NavIdent.of(authService.innloggetVeilederIdent)
            val oppfolgingsbruker = OppfolgingsRegistrering.manuellRegistrering(
                fnr, aktorId,
                VeilederRegistrant(navIdent),
                kontorSattAvVeileder,
                manueltSjekketLovligOpphold
            )
            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker)
        }
    }

    private fun aktiveringKrevdeManuellSjekkAvLovligOppholdEllerThrow(fnr: Fnr): Boolean {
        val fregStatus = pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)
        val fregStatusSjekkResultat = fregStatus.toKanStarteOppfolging()
        return when (fregStatusSjekkResultat) {
            DOD,
            IKKE_LOVLIG_OPPHOLD,
            INGEN_STATUS_FOLKEREGISTERET,
            UKJENT_STATUS_FOLKEREGISTERET -> throw IllegalStateException("Kan ikke starte oppfølging på bruker med folkeregisterstatus $fregStatus -> $fregStatusSjekkResultat")
            FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR -> true
            FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT -> true
            FREG_STATUS_OK -> false
        }
    }
}