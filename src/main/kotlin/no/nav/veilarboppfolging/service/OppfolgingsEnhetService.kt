package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity
import org.springframework.stereotype.Service

@Service
class OppfolgingsEnhetService(
    private val oppfolgingsenhetHistorikkRepository: OppfolgingsenhetHistorikkRepository
) {
    fun getOppfolgingsEnhet(aktorId: AktorId): OppfolgingsenhetEndringEntity? {
        return oppfolgingsenhetHistorikkRepository.hentArenaOppfolgingsenhetForAktorId(aktorId)
    }
}