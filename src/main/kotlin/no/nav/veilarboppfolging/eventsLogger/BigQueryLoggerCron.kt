package no.nav.veilarboppfolging.eventsLogger

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Profile("!test")
@Component
class BigQueryLoggerCron(
    val bigQueryClientKontor: BigQueryClientKontor,
    val kontorMetrikkerDAO: KontorMetrikkerDAO
) {

    @Scheduled(cron = "0 41 9 * * *")
    fun loggAntallAvvikendeArenaOgAoKontorCron() {
        val jobTimestamp = ZonedDateTime.now(ZoneOffset.UTC)

        val arenakontorUtenAoKontor = kontorMetrikkerDAO.hentAvvikendeArenaOgAoKontor()

        bigQueryClientKontor.loggAvvikendeArenaOgAoKontor(arenakontorUtenAoKontor, jobTimestamp)
    }

    @Scheduled(cron = "0 40 9 * * *")
    fun loggOppfolgingsperioderUtenAoKontorCron() {
        val jobTimestamp = ZonedDateTime.now(ZoneOffset.UTC)

        val oppfolgingsperioderUtenAoKontor = kontorMetrikkerDAO.hentOppfolgingsperioderUtenAoKontor()

        bigQueryClientKontor.loggOppfolgingsperioderUtenAoKontor(oppfolgingsperioderUtenAoKontor, jobTimestamp)
    }
}