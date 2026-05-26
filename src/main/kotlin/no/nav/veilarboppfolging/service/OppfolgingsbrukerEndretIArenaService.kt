package no.nav.veilarboppfolging.service

import lombok.RequiredArgsConstructor
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.service.OppfolgingService.kanAvslutteOppfolging
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrElse

@Service
@RequiredArgsConstructor
class OppfolgingsbrukerEndretIArenaService(
    private val oppfolgingService: OppfolgingService,
    private val startOppfolgingService: StartOppfolgingService,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val kvpService: KvpService,
    private val metricsService: MetricsService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
){
    val log = LoggerFactory.getLogger(this.javaClass)

    fun oppdaterOppfolgingMedStatusFraArena(endringOppfolgingsbruker: EndringPaaOppfolgingsBruker) {
        val fnr = Fnr.of(endringOppfolgingsbruker.fodselsnummer)

        val formidlingsgruppe =
            Optional.ofNullable<Formidlingsgruppe>(endringOppfolgingsbruker.formidlingsgruppe).orElse(null)
        val kvalifiseringsgruppe =
            Optional.ofNullable<Kvalifiseringsgruppe>(endringOppfolgingsbruker.kvalifiseringsgruppe).orElse(null)

        val currentLocalOppfolging: Optional<OppfolgingEntity> =
            oppfolgingsStatusRepository.hentOppfolging(endringOppfolgingsbruker.aktorId)

        val erBrukerUnderOppfolgingLokalt: Boolean = currentLocalOppfolging.map { it.isUnderOppfolging }.getOrElse { false }
        val erUnderOppfolgingIArena = ArenaUtils.erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe)
        val erInaktivIArena = ArenaUtils.erIserv(formidlingsgruppe)

        fun kanAvsluttes(kanEnkeltReaktiveres: Boolean): OppfolgingService.KanAvslutteMedBegrunnelse {
            val erUnderKvp = kvpService.erUnderKvp(endringOppfolgingsbruker.aktorId)
            val harAktiveTiltaksdeltakelser = oppfolgingService.harAktiveTiltaksdeltakelser(fnr)
            val erDeltakerIUngdomsprogrammet = oppfolgingService.erDeltakerIUngdomsprogrammet(fnr)
            val erArbeidssoeker = oppfolgingService.erArbeidssoeker(fnr)
            val harAap = oppfolgingService.harAap(fnr)

            val kanAvsluttesMedBegrunnelse = kanAvslutteOppfolging(
                endringOppfolgingsbruker.aktorId,
                AvregistreringsType.ArenaIservKanIkkeReaktiveres,
                erBrukerUnderOppfolgingLokalt,
                erInaktivIArena,
                harAktiveTiltaksdeltakelser,
                erDeltakerIUngdomsprogrammet,
                erArbeidssoeker,
                harAap,
                erUnderKvp,
            )

            val kanAvsluttes = !kanEnkeltReaktiveres && kanAvsluttesMedBegrunnelse.kanAvslutte

            secureLog.info(
                "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} harAktiveTiltaksdeltakelser={} erDeltakerIUngdomsprogrammet={} erArbeidssoeker={} harAap={} kanAvsluttes={}",
                endringOppfolgingsbruker.aktorId,
                kanEnkeltReaktiveres,
                erUnderKvp,
                harAktiveTiltaksdeltakelser,
                erDeltakerIUngdomsprogrammet,
                erArbeidssoeker,
                harAap,
                kanAvsluttes
            )

            if (kanEnkeltReaktiveres) {
                return OppfolgingService.KanAvslutteMedBegrunnelse( false,"Bruker kan enkelt reaktiveres i Arena, og vil derfor ikke automatisk avsluttes")
            }
            return kanAvsluttesMedBegrunnelse
        }

        val harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty
        oppfolgingService.oppdaterArenaOppfolgingStatus(
            endringOppfolgingsbruker.aktorId,
            harIngenOppfolgingLagret,
            LocalArenaOppfolging(
                endringOppfolgingsbruker.hovedmaal,
                kvalifiseringsgruppe,
                formidlingsgruppe,
                Optional.ofNullable<String?>(endringOppfolgingsbruker.oppfolgingsenhet)
                    .map { id -> EnhetId(id) }.orElse(null),
                endringOppfolgingsbruker.iservFraDato
            )
        )

        val hendelse = resolveEndringPaaOppfolgingsbrukerEvent(
            endringOppfolgingsbruker,
            currentLocalOppfolging.orElse(null),
            { arenaOppfolgingService.kanEnkeltReaktiveres(fnr) },
            ::kanAvsluttes
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
                secureLog.info(
                    "Automatisk avslutting av oppfølging på bruker. aktorId={}",
                    endringOppfolgingsbruker.aktorId
                )
                log.info("Utgang: Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres")
                val avregistrering = ArenaIservKanIkkeReaktiveres(endringOppfolgingsbruker.aktorId)
                oppfolgingService.avsluttOppfolging(avregistrering)
                metricsService.rapporterAutomatiskAvslutningAvOppfolging(true)
            }
            else -> {}
        }

        log.info("Endring pa oppfolgingsbruker - ${hendelse.loggMessage()}")

        secureLog.info(
            ("Status for automatisk oppdatering av oppfølging."
                    + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                    + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                    + " formidlingsgruppe={} kvalifiseringsgruppe={}"),
            endringOppfolgingsbruker.aktorId, erBrukerUnderOppfolgingLokalt,
            erUnderOppfolgingIArena, erInaktivIArena,
            formidlingsgruppe, kvalifiseringsgruppe
        )
    }
}
