package no.nav.veilarboppfolging.service

import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.InaktivertIArena
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class OppfolgingsbrukerEndretIArenaService(
    private val oppfolgingService: OppfolgingService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
){
    val log = LoggerFactory.getLogger(this.javaClass)

    fun oppdaterOppfolgingMedStatusFraArena(endringOppfolgingsbruker: EndringPaaOppfolgingsBruker) {
        val currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(endringOppfolgingsbruker.aktorId)
        oppdaterLokalArenaOppfolging(endringOppfolgingsbruker, currentLocalOppfolging)
        val hendelse = resolveEndringPaaOppfolgingsbrukerEvent(
            endringOppfolgingsbruker.formidlingsgruppe,
            currentLocalOppfolging.orElse(null),
        )

        when (hendelse) {
            is BleInaktivertVarArbs -> {
                secureLog.info("Bruker gikk fra ARBS til ISERV. aktorId={}", endringOppfolgingsbruker.aktorId)
                log.info("Oppdaterer ikke utmeldingstabell for bruker som gikk fra ARBS til ISERV")
            }
            is BleInaktivertVarIarbs -> {
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
            is IrrelevantEndring -> {}
        }

        log.info("Endring pa oppfolgingsbruker - ${hendelse.loggMessage()}")

        secureLog.info(
            ("Status for automatisk oppdatering av oppfølging."
                    + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                    + " erInaktivIArena={}"
                    + " formidlingsgruppe={} kvalifiseringsgruppe={}"),
            endringOppfolgingsbruker.aktorId,
            currentLocalOppfolging.getOrNull()?.underOppfolging ?: false,
            ArenaUtils.erIserv(endringOppfolgingsbruker.formidlingsgruppe),
            endringOppfolgingsbruker.formidlingsgruppe,
            endringOppfolgingsbruker.kvalifiseringsgruppe
        )
    }

    private fun oppdaterLokalArenaOppfolging(
        endringOppfolgingsbruker: EndringPaaOppfolgingsBruker,
        currentLocalOppfolging: Optional<OppfolgingEntity>
    ) {
        val harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty
        oppfolgingService.oppdaterArenaOppfolgingStatus(
            endringOppfolgingsbruker.aktorId,
            harIngenOppfolgingLagret,
            LocalArenaOppfolging(
                endringOppfolgingsbruker.hovedmaal,
                endringOppfolgingsbruker.kvalifiseringsgruppe,
                endringOppfolgingsbruker.formidlingsgruppe,
                endringOppfolgingsbruker.iservFraDato
            )
        )
    }
}
