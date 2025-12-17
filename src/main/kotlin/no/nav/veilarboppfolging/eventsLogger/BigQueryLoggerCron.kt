package no.nav.veilarboppfolging.eventsLogger

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.ZoneOffset
import java.time.ZonedDateTime

@EnableScheduling
class BigQueryLoggerCron(
    val bigQueryClientKontor: BigQueryClientKontor,
    val kontorMetrikkerDAO: KontorMetrikkerDAO
) {

    @Scheduled(cron = "@midnight")
    fun hentAntallAvvikendeArenaOgAoKontorCron() {
        val jobTimestamp = ZonedDateTime.now(ZoneOffset.UTC)

        val arenakontorUtenAoKontor = kontorMetrikkerDAO.hentAvvikendeArenaOgAoKontor()

        bigQueryClientKontor.loggAvvikendeArenaOgAoKontor(arenakontorUtenAoKontor, jobTimestamp)
    }
}