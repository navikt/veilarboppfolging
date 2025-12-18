package no.nav.veilarboppfolging.eventsLogger

import org.slf4j.LoggerFactory
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
    val log = LoggerFactory.getLogger(BigQueryLoggerCron::class.java)

    @Scheduled(cron = "0 58 9 * * *")
    fun loggAntallAvvikendeArenaOgAoKontorCron() {
        log.info("Starter cron for å logge avvikende arena og ao kontor til BigQuery")
        val arenakontorUtenAoKontor = kontorMetrikkerDAO.hentAvvikendeArenaOgAoKontor()
        log.info("Fant ${arenakontorUtenAoKontor.size} avvikende arena og ao kontor")

        val jobTimestamp = ZonedDateTime.now(ZoneOffset.UTC)
        bigQueryClientKontor.loggAvvikendeArenaOgAoKontor(arenakontorUtenAoKontor, jobTimestamp)
    }

    @Scheduled(cron = "0 57 9 * * *")
    fun loggOppfolgingsperioderUtenAoKontorCron() {
        log.info("Starter cron for å logge oppfølgingsperioder uten ao kontor til BigQuery")
        val oppfolgingsperioderUtenAoKontor = kontorMetrikkerDAO.hentOppfolgingsperioderUtenAoKontor()
        log.info("Fant ${oppfolgingsperioderUtenAoKontor.size} oppfølgingsperioder uten ao kontor")

        val jobTimestamp = ZonedDateTime.now(ZoneOffset.UTC)
        bigQueryClientKontor.loggOppfolgingsperioderUtenAoKontor(oppfolgingsperioderUtenAoKontor, jobTimestamp)
    }
}