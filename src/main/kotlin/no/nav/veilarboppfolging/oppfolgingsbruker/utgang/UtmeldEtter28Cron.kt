package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import java.time.Instant
import kotlin.jvm.optionals.getOrNull
import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.InaktivertIArena
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class UtmeldEtter28Cron(
    private val utmeldingService: UtmeldingsService,
    private val utmeldingsRepository: UtmeldingRepository,
    private val leaderElectionClient: LeaderElectionClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
    private val transactor: TransactionTemplate,
    private val oppfolgingService: OppfolgingService,
) {
    private val log = LoggerFactory.getLogger(UtmeldEtter28Cron::class.java)
    private val BATCH_SIZE = 1000

    enum class AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    @Scheduled(cron = "0 30 * * * *")
    fun migrerFraUtmeldingstabell() {
        if (!leaderElectionClient.isLeader) {
            return
        }
        JobRunner.run("migrer_fra_utmeldingstabell") {
            migrerBrukereFraGammelUtmeldingstabell()
        }
    }

    fun migrerBrukereFraGammelUtmeldingstabell() {
        var currentOffset = 0
        while (true) {
            val alleBrukere = utmeldingsRepository.hentAlleBrukere(currentOffset, BATCH_SIZE)
            if (alleBrukere.isEmpty()) {
                break
            }

            log.info(
                "Migrerer brukere fra gammel utmeldingstabell. CurrentOffset={} BatchSize={}",
                currentOffset,
                alleBrukere.size,
            )

            currentOffset += alleBrukere.size

            alleBrukere.forEach { utmeldingEntity ->
                try {
                    transactor.executeWithoutResult { _ ->
                        val aktorId = AktorId.of(utmeldingEntity.aktorId)
                        val gjeldendeOppfolgingsperiode =
                            oppfolgingService.hentGjeldendeOppfolgingsperiode(aktorId).getOrNull()
                        if (gjeldendeOppfolgingsperiode == null) {
                            log.info("Bruker har ingen gjeldende oppfølgingsperiode, fjerner fra utmeldingstabellen")
                            slettBrukerFraUtmeldingstabell(aktorId)
                            return@executeWithoutResult
                        }
                        val oppfolgingsperiodeId = gjeldendeOppfolgingsperiode.uuid
                        if (utmeldingEntity.iservSiden.isBefore(gjeldendeOppfolgingsperiode.startDato)) {
                            log.info("Bruker har startet ny oppfølgingsperiode med id $oppfolgingsperiodeId etter iserv-datoen, fjerner fra utmeldingstabellen")
                            slettBrukerFraUtmeldingstabell(aktorId)
                            return@executeWithoutResult
                        }

                        val erKandidatForUtmelding =
                            kandidatForUtmeldingService.erAktivEllerForlengetKandidatForUtmelding(oppfolgingsperiodeId)
                        if (erKandidatForUtmelding) {
                            log.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId er allerede kandidat for utmelding, fjerner fra utmeldingstabellen")
                            slettBrukerFraUtmeldingstabell(aktorId)
                            return@executeWithoutResult
                        }

                        val kandidatSomIkkeKanAvsluttes =
                            kandidatForUtmeldingService.erLagretSomKandidatSomIkkeKanAvsluttes(oppfolgingsperiodeId)
                        if (kandidatSomIkkeKanAvsluttes) {
                            log.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId er allerede lagret som kandidat som ikke kan avsluttes, fjerner fra utmeldingstabellen")
                            slettBrukerFraUtmeldingstabell(aktorId)
                            return@executeWithoutResult
                        }

                        log.info("Lagrer inaktivert i arena-hendelse og fjerner bruker med oppfølgingsperiode $oppfolgingsperiodeId fra utmeldingstabellen")
                        kandidatForUtmeldingService.handterUtmeldingsHendelse(
                            hendelse = InaktivertIArena(
                                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
                                iservFraDato = utmeldingEntity.iservSiden.toLocalDate(),
                                hendelseTidspunkt = utmeldingEntity.iservSiden.toInstant() ?: Instant.now(),
                            )
                        )
                        slettBrukerFraUtmeldingstabell(aktorId)
                    }
                } catch (e: Exception) {
                    log.error("Feil ved migrering av bruker fra utmeldingstabell", e)
                    secureLog.error("Feil ved migrering av bruker fra utmeldingstabell. aktorId=${utmeldingEntity.aktorId}", e)
                }
            }
        }
        log.info("Ferdig med å migrere $currentOffset brukere")
    }

    private fun slettBrukerFraUtmeldingstabell(aktorId: AktorId) {
        val resultat = utmeldingService.slettFraUtmeldingTabell(aktorId)
        if (resultat == AvslutteOppfolgingResultat.AVSLUTTET_FEILET) {
            throw RuntimeException("Feil ved sletting av bruker fra utmeldingstabell")
        }
    }
}
