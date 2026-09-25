package no.nav.veilarboppfolging.service

import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.InaktivertIArena
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppfolgingsbrukerEndretIArenaService(
    private val oppfolgingService: OppfolgingService,
    private val startOppfolgingService: StartOppfolgingService,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
){
    val log = LoggerFactory.getLogger(this.javaClass)

    fun oppdaterOppfolgingMedStatusFraArena(endringOppfolgingsbruker: EndringPaaOppfolgingsBruker) {
        val fnr = Fnr.of(endringOppfolgingsbruker.fodselsnummer)

        val formidlingsgruppe = endringOppfolgingsbruker.formidlingsgruppe
        val kvalifiseringsgruppe = endringOppfolgingsbruker.kvalifiseringsgruppe
        val erInaktivIArena = ArenaUtils.erIserv(formidlingsgruppe)

        val currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(endringOppfolgingsbruker.aktorId)
        val erBrukerUnderOppfolgingLokalt = currentLocalOppfolging.getOrNull()?.underOppfolging ?: false

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
                if (erUnder18(fnr)) {
                    secureLog.info("Bruker er under 18 år, starter ikke oppfølging. aktorId={}", endringOppfolgingsbruker.aktorId)
                    log.info("Bruker er under 18 år, starter ikke oppfølging.")
                    return
                }
                startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                    arenaSyncOppfolgingBrukerRegistrering(
                        fnr,
                        endringOppfolgingsbruker.aktorId,
                        formidlingsgruppe,
                        kvalifiseringsgruppe,
                    )
                )
            }
            is VarArbsBleIserv -> {
                secureLog.info("Bruker gikk fra ARBS til ISERV. aktorId={}", endringOppfolgingsbruker.aktorId)
                log.info("Oppdaterer ikke utmeldingstabell for bruker som gikk fra ARBS til ISERV")
            }
            is BleInaktivertMedKanReaktiveres,
            is BleInaktivertUtenKanReaktiveres -> {
                val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(endringOppfolgingsbruker.aktorId).getOrElse {
                    log.error("Fant ikke oppfølgingsperiode for bruker som ble inaktivert i Arena")
                    throw IllegalStateException("Fant ikke oppfølgingsperiode for bruker som ble inaktivert i Arena")
                }.uuid
                kandidatForUtmeldingService.handterUtmeldingsHendelse(
                    fnr = Fnr.of(endringOppfolgingsbruker.fodselsnummer),
                    hendelse = InaktivertIArena(
                        oppfolgingsperiodeUuid = oppfolgingsperiodeId,
                        iservFraDato = endringOppfolgingsbruker.iservFraDato,
                        hendelseTidspunkt = endringOppfolgingsbruker.iservFraDato?.atStartOfDay()
                            ?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now(),
                    )
                )
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

    private fun erUnder18(fnr: Fnr): Boolean {
        val folkeregisterstatus = pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)
        return folkeregisterstatus.under18
    }
}
