package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import org.springframework.stereotype.Service

@Service
class ArbeidsoppfolgingsKontorService(
    val arbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository,
    val aktorOppslagClient: AktorOppslagClient,
) {
    fun hentOppfolgingsEnhetId(fnr: Fnr): EnhetId? {
        val alleFnr = aktorOppslagClient.hentIdenter(fnr).let { it.historiskeFnr + it.fnr }
        return arbeidsoppfolgingskontorRepository.hentEnhet(alleFnr)
    }
}
