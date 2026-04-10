package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldEtter28Cron.AvslutteOppfolgingResultat
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.MetricsService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UtmeldingsService(
    val metricsService: MetricsService,
    val utmeldingRepository: UtmeldingRepository,
    val oppfolgingService: OppfolgingService,
    val bigQueryClient: BigQueryClient
) {
    private val log = LoggerFactory.getLogger(UtmeldingsService::class.java)

    fun oppdaterUtmeldingsStatus(bruker: KanskjeIservBruker) {
        val hendelse = bruker.resolveUtmeldingsHendelse(
            { oppfolgingService.erUnderOppfolging(bruker.aktorId) },
            { finnesIUtmeldingTabell(bruker.aktorId) })

        when (hendelse) {
            is UpsertIUtmelding -> {
                secureLog.info("Oppdaterer eller insert i utmelding tabell. aktorId={}", bruker.aktorId)
                upsertUtmeldingTabell(hendelse)
            }
            is SlettFraUtmelding -> {
                secureLog.info("Sletter fra utmelding tabell. aktorId={}", bruker.aktorId)
                slettFraUtmeldingTabell(hendelse)
            }
            is NoOp -> {
                secureLog.info("Ingen endring i utmelding tabell. aktorId={}", bruker.aktorId)
            }
        }
    }

    private fun slettFraUtmeldingTabell(utmeldingHendelse: SlettFraUtmelding) {
        bigQueryClient.loggUtmeldingsHendelse(utmeldingHendelse)
        utmeldingRepository.slettBrukerFraUtmeldingTabell(utmeldingHendelse.aktorId)
    }

    private fun upsertUtmeldingTabell(event: UpsertIUtmelding) {
        bigQueryClient.loggUtmeldingsHendelse(event)
        when (event) {
            is UpdateIservDatoUtmelding -> utmeldingRepository.updateUtmeldingTabell(event)
            is InsertIUtmelding -> utmeldingRepository.insertUtmeldingTabell(event)
        }
    }

    private fun finnesIUtmeldingTabell(aktorId: AktorId): Boolean {
        return utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent
    }

    fun avsluttOppfolgingOgFjernFraUtmeldingsTabell(aktorId: AktorId): AvslutteOppfolgingResultat {
        try {
            if (!oppfolgingService.erUnderOppfolging(aktorId)) {
                secureLog.info(
                    "Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell",
                    aktorId
                )
                slettFraUtmeldingTabell(ScheduledJob_AlleredeUteAvOppfolging(aktorId))
                return AvslutteOppfolgingResultat.IKKE_LENGER_UNDER_OPPFØLGING
            } else {
                log.info("Utgang: Forsøker å avslutte oppfølging automatisk grunnet iserv i 28 dager")
                val avregistrering = UtmeldtEtter28Dager(aktorId)
                val avslutningStatus = oppfolgingService.avsluttOppfolging(avregistrering)
                // Hvis kanAvslutte er false, så betyr det at oppfølgingsperioden hos oss ikke ble avsluttet.
                // Da beholder vi brukeren i utmeldingstabellen, og forsøker igjen senere
                val oppfolgingFaktiskAvsluttet = avslutningStatus.kanAvslutte
                if (oppfolgingFaktiskAvsluttet) {
                    slettFraUtmeldingTabell(ScheduledJob_UtAvOppfolgingPga28DagerIserv(aktorId))
                    metricsService.antallBrukereAvsluttetAutomatisk()
                }
                return when {
                    oppfolgingFaktiskAvsluttet -> AvslutteOppfolgingResultat.AVSLUTTET_OK
                    else -> AvslutteOppfolgingResultat.IKKE_AVSLUTTET
                }
            }
        } catch (e: Exception) {
            secureLog.error("Automatisk avsluttOppfolging feilet for aktoerid $aktorId ", e)
            return AvslutteOppfolgingResultat.AVSLUTTET_FEILET
        }
    }

}
