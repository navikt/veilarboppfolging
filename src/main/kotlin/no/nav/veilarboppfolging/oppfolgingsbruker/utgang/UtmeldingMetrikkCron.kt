package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.UtmeldingsAntall
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class UtmeldingMetrikkCron(
    private val utmeldingsRepository: UtmeldingRepository,
    private val bigQueryClient: BigQueryClient
) {

    @Scheduled
    fun countAntallBrukerePaaVeiUtAvOppfolging() {
        val antall = utmeldingsRepository.tellBrukereUnderOppfolgingIGracePeriode()
        bigQueryClient.loggUtmeldingsCount(UtmeldingsAntall(
            personIUtmeldingSomErUnderOppfolging = antall
        ))
    }

}
