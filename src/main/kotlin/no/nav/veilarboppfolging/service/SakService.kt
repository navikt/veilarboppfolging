package no.nav.veilarboppfolging.service

import no.nav.veilarboppfolging.repository.SakEntity
import no.nav.veilarboppfolging.repository.SakRepository
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.util.*

@Service
class SakService(private val sakRepository: SakRepository) {

    fun hentSak(oppfølgingsperiodeUUID: UUID): SakEntity {
        val saker = sakRepository.hentSaker(oppfølgingsperiodeUUID)

        return when {
            saker.isEmpty() -> {
                // OracleDB gjør det ikke lett å hente ut genererte ID-er, så gjør lagring og uthenting i to steg
                sakRepository.opprettSak(oppfølgingsperiodeUUID)
                sakRepository.hentSaker(oppfølgingsperiodeUUID).first().also { nySak ->
                    secureLog.info("Opprettet sak med ID ${nySak.id} for oppfølgingsperiode $oppfølgingsperiodeUUID")
                }
            }
            saker.size == 1 -> saker.first()
            else -> throw IllegalStateException("Det finnes flere saker på samme oppfølgingsperiode. Dette skulle ikke ha skjedd.")
        }
    }
}