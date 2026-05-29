package no.nav.veilarboppfolging.service

import lombok.RequiredArgsConstructor
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneAvsluttes
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneIkkeAvsluttes
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse

@Service
@RequiredArgsConstructor
class OppfolgingsbrukerEndretIArenaService(
    private val oppfolgingService: OppfolgingService,
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val startOppfolgingService: StartOppfolgingService,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val metricsService: MetricsService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
){
    val log = LoggerFactory.getLogger(this.javaClass)

    fun oppdaterOppfolgingMedStatusFraArena(endringOppfolgingsbruker: EndringPaaOppfolgingsBruker) {
        val fnr = Fnr.of(endringOppfolgingsbruker.fodselsnummer)

        val formidlingsgruppe = endringOppfolgingsbruker.formidlingsgruppe
        val kvalifiseringsgruppe = endringOppfolgingsbruker.kvalifiseringsgruppe
        val erInaktivIArena = ArenaUtils.erIserv(formidlingsgruppe)

        val currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(endringOppfolgingsbruker.aktorId)
        val erBrukerUnderOppfolgingLokalt = currentLocalOppfolging.map { it.isUnderOppfolging }.getOrElse { false }

        val harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty
        oppfolgingService.oppdaterArenaOppfolgingStatus(
            endringOppfolgingsbruker.aktorId,
            harIngenOppfolgingLagret,
            LocalArenaOppfolging(
                endringOppfolgingsbruker.hovedmaal,
                kvalifiseringsgruppe,
                formidlingsgruppe,
                endringOppfolgingsbruker.iservFraDato
            )
        )

        val hendelse = resolveEndringPaaOppfolgingsbrukerEvent(
            endringOppfolgingsbruker,
            currentLocalOppfolging.orElse(null),
            { arenaOppfolgingService.kanEnkeltReaktiveres(fnr) },
        )

        when (hendelse) {
            is BleSykmeldtUtenArbeidsgiver -> {
                secureLog.info(
                    "Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}",
                    endringOppfolgingsbruker.aktorId
                )
                startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                    arenaSyncOppfolgingBrukerRegistrering(
                        fnr,
                        endringOppfolgingsbruker.aktorId,
                        formidlingsgruppe,
                        kvalifiseringsgruppe,
                        EnhetId.of(endringOppfolgingsbruker.oppfolgingsenhet)
                    )
                )
            }
            is BleInaktivertUtenKanReaktiveres -> {
                val avregistrering = ArenaIservKanIkkeReaktiveres(endringOppfolgingsbruker.aktorId)
                val kunneAvsluttesResultat = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(avregistrering)
                when (kunneAvsluttesResultat) {
                    is KunneAvsluttes -> {
                        secureLog.info(
                            "Automatisk avslutting av oppfølging på bruker. aktorId={}",
                            endringOppfolgingsbruker.aktorId
                        )
                        log.info("Utgang: Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres")
                        metricsService.rapporterAutomatiskAvslutningAvOppfolging(true)
                    }
                    is KunneIkkeAvsluttes -> {
                        // TODO Logl litt her
                    }
                }
            }
            else -> {}
        }

        log.info("Endring pa oppfolgingsbruker - ${hendelse.loggMessage()}")

        secureLog.info(
            ("Status for automatisk oppdatering av oppfølging."
                    + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                    + " erInaktivIArena={}"
                    + " formidlingsgruppe={} kvalifiseringsgruppe={}"),
            endringOppfolgingsbruker.aktorId, erBrukerUnderOppfolgingLokalt,
            erInaktivIArena, formidlingsgruppe, kvalifiseringsgruppe
        )
    }
}
