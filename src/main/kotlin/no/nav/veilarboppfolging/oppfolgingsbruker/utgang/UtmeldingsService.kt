package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldEtter28Cron.AvslutteOppfolgingResultat
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.MetricsService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import no.nav.veilarboppfolging.service.utmelding.UtmeldingsBruker
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId

@Service
class UtmeldingsService(
    val metricsService: MetricsService,
    val utmeldingRepository: UtmeldingRepository,
    val oppfolgingService: OppfolgingService
) {
    private val log = LoggerFactory.getLogger(UtmeldingsService::class.java)

    fun oppdaterUtmeldingsStatus(kanskjeIservBruker: KanskjeIservBruker, aktorId: AktorId) {
        if (kanskjeIservBruker.erIserv()) {
            secureLog.info("Oppdaterer eller insert i utmelding tabell. aktorId={}", aktorId)
            oppdaterUtmeldingTabell(kanskjeIservBruker.utmeldingsBruker(), aktorId)
        } else {
            secureLog.info("Sletter fra utmelding tabell. aktorId={}", aktorId)
            slettFraUtmeldingTabell(IkkeLengerIservIArena(aktorId))
        }
    }

    private fun slettFraUtmeldingTabell(utmeldingHendelse: SlettFraUtmelding) {
        when (utmeldingHendelse) {
            is IkkeLengerIservIArena -> utmeldingRepository.slettBrukerFraUtmeldingTabell(utmeldingHendelse.aktorId)
            is ManueltAvsluttetAvVeielder -> utmeldingRepository.slettBrukerFraUtmeldingTabell(utmeldingHendelse.aktorId)
            is UtAvOppfolgingPga28DagerIserv -> utmeldingRepository.slettBrukerFraUtmeldingTabell(utmeldingHendelse.aktorId)
            is AlleredeUteAvOppfolging -> utmeldingRepository.slettBrukerFraUtmeldingTabell(utmeldingHendelse.aktorId)
        }
    }

    private fun oppdaterUtmeldingTabell(utmeldingsBruker: UtmeldingsBruker, aktorId: AktorId) {
        val iservFraDato = utmeldingsBruker.iservFraDato
        if (iservFraDato == null) {
            secureLog.error("Kan ikke oppdatere utmeldingstabell med bruker siden iservFraDato mangler. aktorId={}", aktorId);
            throw IllegalArgumentException("iservFraDato mangler på EndringPaaOppfoelgingsBrukerV2");
        }

        if (finnesIUtmeldingTabell(aktorId)) {
            utmeldingRepository.updateUtmeldingTabell(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
        } else if (oppfolgingService.erUnderOppfolging(aktorId)) {
            utmeldingRepository.insertUtmeldingTabell(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
        }
    }

    private fun finnesIUtmeldingTabell(aktorId: AktorId): Boolean {
        return utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent
    }

    fun avsluttOppfolgingOfFjernFraUtmeldingsTabell(aktorId: AktorId): AvslutteOppfolgingResultat {
        try {
            if (!oppfolgingService.erUnderOppfolging(aktorId)) {
                secureLog.info(
                    "Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell",
                    aktorId
                )
                slettFraUtmeldingTabell(AlleredeUteAvOppfolging(aktorId))
                return AvslutteOppfolgingResultat.IKKE_LENGER_UNDER_OPPFØLGING
            } else {
                log.info("Utgang: Oppfølging avsluttet automatisk grunnet iserv i 28 dager")
                val avregistrering = UtmeldtEtter28Dager(aktorId)
                val avslutningStatus = oppfolgingService.avsluttOppfolging(avregistrering)
                // TODO litt i tvil om denne her. Attributtet sier om du per def er under oppfølging i arena, ikke om du er under oppfølging hos oss.
                val oppfolgingAvsluttet = !avslutningStatus.underOppfolging
                if (oppfolgingAvsluttet) {
                    slettFraUtmeldingTabell(UtAvOppfolgingPga28DagerIserv(aktorId))
                    metricsService.antallBrukereAvsluttetAutomatisk()
                }
                return when {
                    oppfolgingAvsluttet -> AvslutteOppfolgingResultat.AVSLUTTET_OK
                    else -> AvslutteOppfolgingResultat.IKKE_AVSLUTTET
                }
            }
        } catch (e: Exception) {
            secureLog.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktorId, e)
            return AvslutteOppfolgingResultat.AVSLUTTET_FEILET
        }
    }

}