package no.nav.veilarboppfolging.service

import no.nav.veilarboppfolging.repository.SakEntity
import no.nav.veilarboppfolging.repository.SakRepository
import no.nav.veilarboppfolging.repository.SakStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
class SakService(private val sakRepository: SakRepository) {

    // TODO: Trenger vi å sjekke mot brukers fnr?
    fun hentÅpenSak(oppfølgingsperiodeUUID: UUID): SakEntity {
        val saker = sakRepository.hentSaker(oppfølgingsperiodeUUID)

        return if (saker.isEmpty()) {
            sakRepository.opprettSak(oppfølgingsperiodeUUID)
            sakRepository.hentSaker(oppfølgingsperiodeUUID).first()
        } else {
            saker
                .filter { it.status != SakStatus.AVSLUTTET }
                .sortedBy { it.createdAt } // TODO: Test sortering
                .first()
        }
    }
}