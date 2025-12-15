package no.nav.veilarboppfolging.eventsLogger

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@EnableScheduling
class BigQueryLoggerCron {

    @Scheduled(cron = "@midnight")
    fun hentAntallAvvikendeArenaOgAoKontorCron() {

    }
}