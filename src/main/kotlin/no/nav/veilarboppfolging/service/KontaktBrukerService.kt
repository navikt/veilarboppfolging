package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.controller.KontaktBrukerDto
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.oppgave.OppgaveClient
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.springframework.stereotype.Service

@Service
class KontaktBrukerService(
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient,
    private val aktorOppslagClient: AktorOppslagClient,
    private val oppgaveClient: OppgaveClient,
) {
    fun opprettOppgave(fnr: Fnr): KontaktBrukerDto {
        if (!erUnder18Aar(fnr)) {
            throw IllegalArgumentException("Bruker er over 18 år og skal ikke kunne be om å bli kontaktet")
        }
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        if (erUnderOppfolging(aktorId)) {
            throw IllegalArgumentException("Bruker er under oppfølging og skal ikke kunne be om å bli kontaktet")
        }

        return KontaktBrukerDto(frist = oppgaveClient.opprettOppgave(fnr, aktorId))
    }

    private fun erUnderOppfolging(aktorId: AktorId): Boolean {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map { it.isUnderOppfolging }.orElse(false)
    }

    private fun erUnder18Aar(fnr: Fnr): Boolean {
        val folkeregisterStatus = pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)
        return folkeregisterStatus.under18
    }
}