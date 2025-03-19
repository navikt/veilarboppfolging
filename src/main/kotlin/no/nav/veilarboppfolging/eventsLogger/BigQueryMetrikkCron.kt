package no.nav.veilarboppfolging.eventsLogger

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
open class BigQueryMetrikkCron(
    val bigQueryClient: BigQueryClient,
    private val antallRegistrertInngarDAO: AntallRegistrertInngarDAO
) {

    @Scheduled(cron = "@midnight")
    open fun hentAntallRegistrerteInngarCron() {
        val antallRegistrerteInngar = antallRegistrertInngarDAO.hentAntallRegistrertMedInngar()
        bigQueryClient.
    }
}