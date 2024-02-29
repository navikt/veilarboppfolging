package no.nav.veilarboppfolging.service

import no.nav.veilarboppfolging.repository.SakEntity
import no.nav.veilarboppfolging.repository.SakRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class SakService(private val sakRepository: SakRepository) {

    fun hentSak(oppfølgingsperiodeUUID: UUID): SakEntity {
        val saker = sakRepository.hentSaker(oppfølgingsperiodeUUID)

        return when {
            saker.isEmpty() -> sakRepository.opprettSak(oppfølgingsperiodeUUID)
            saker.size == 1 -> saker.first()
            else -> throw IllegalStateException("Det finnes flere saker på samme oppfølgingsperiode. Dette skulle ikke ha skjedd.")
        }
    }
}