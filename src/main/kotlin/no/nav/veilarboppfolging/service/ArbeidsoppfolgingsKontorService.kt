package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import org.springframework.stereotype.Service

@Service
class ArbeidsoppfolgingsKontorService(
    val arbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository,
) {
    fun hentOppfolgingsEnhetId(fnr: Fnr): EnhetId? {
        return arbeidsoppfolgingskontorRepository.hentEnhet(fnr)
    }
}
