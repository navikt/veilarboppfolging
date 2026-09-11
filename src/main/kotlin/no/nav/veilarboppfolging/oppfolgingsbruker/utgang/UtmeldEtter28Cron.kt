package no.nav.veilarboppfolging.oppfolgingsbruker.utgang


import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.utils.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class UtmeldEtter28Cron(
    private val utmeldingService: UtmeldingsService,
    private val utmeldingsRepository: UtmeldingRepository,
    private val leaderElectionClient: LeaderElectionClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
    @Value("\${app.utmeldingskandidater_aktivert}") private val utmeldingsKandidaterAktivert: Boolean,
) {
    private val log = LoggerFactory.getLogger(UtmeldEtter28Cron::class.java)

    enum class AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    @Scheduled(cron = "0 0 * * * *")
    fun scheduledAvslutteOppfolgingEtter28DagerIUtmelding() {
        if (leaderElectionClient.isLeader) {
            JobRunner.run(
                "iserv28_avslutt_oppfolging",
                UnsafeRunnable { automatiskAvslutteOppfolging()
            })
        }
    }

    fun automatiskAvslutteOppfolging() {
        val start = System.currentTimeMillis()
        val resultater = finnBrukereOgAvslutt()
        log.info(
            "Avslutter jobb for automatisk avslutning av brukere. Tid brukt: {} ms. Antall [Avsluttet/Ikke avsluttet/Ikke lenger under oppfølging/Feilet/Totalt]: [{}/{}/{}/{}/{}]",
            System.currentTimeMillis() - start,
            resultater.count { it == AvslutteOppfolgingResultat.AVSLUTTET_OK },
            resultater.count { it == AvslutteOppfolgingResultat.IKKE_AVSLUTTET },
            resultater.count { it == AvslutteOppfolgingResultat.IKKE_LENGER_UNDER_OPPFØLGING },
            resultater.count { it == AvslutteOppfolgingResultat.AVSLUTTET_FEILET },
            resultater.size
        )
    }

    private fun finnBrukereOgAvslutt(): List<AvslutteOppfolgingResultat> {
        try {
            log.info("Starter jobb for automatisk avslutning av brukere")
            val iservert28DagerBrukere = utmeldingsRepository.finnBrukereMedIservI28Dager()
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size)
            return iservert28DagerBrukere.map { utmeldingEntity ->
                val aktorId = AktorId.of(utmeldingEntity.aktorId)
                when (utmeldingsKandidaterAktivert && kandidatForUtmeldingService.erUtmeldingskandidat(aktorId)) {
                    true -> {
                        log.info("Bruker var kandidat for utmelding, sletter fra gammel utmeldingsløsning")
                        utmeldingService.slettFraUtmeldingTabell(aktorId)
                    }
                    false -> utmeldingService.avsluttOppfolgingOgFjernFraUtmeldingsTabell(aktorId)
                }
            }
        } catch (e: Exception) {
            SecureLog.secureLog.error("Feil ved automatisk avslutning av brukere", e)
            return emptyList()
        }
    }

}
