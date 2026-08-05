package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO
import no.nav.veilarboppfolging.controller.response.VeilederTilgang
import no.nav.veilarboppfolging.domain.Oppfolging
import no.nav.veilarboppfolging.domain.OppfolgingStatusData
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.repository.BrukerOppslagFlereOppfolgingAktorRepository
import no.nav.veilarboppfolging.repository.KvpRepository
import no.nav.veilarboppfolging.repository.MaalRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.MaalEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.EnumUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

@Service
class OppfolgingService @Autowired constructor(
    private val kvpService: KvpService,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val authService: AuthService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,  // TODO: Når vi får splittet servicenen bedre så skal det ikke være behov for å bruke @Lazy
    @param:Lazy private val manuellStatusService: ManuellStatusService,
    private val kvpRepository: KvpRepository,
    private val maalRepository: MaalRepository,
    private val brukerOppslagFlereOppfolgingAktorRepository: BrukerOppslagFlereOppfolgingAktorRepository,
    private val arbeidsoppfolgingsKontorService: ArbeidsoppfolgingsKontorService,
    private val tiltakshistorikkClient: TiltakshistorikkClient
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional // TODO: kan denne være read only?
    fun hentOppfolgingsStatus(fnr: Fnr): OppfolgingStatusData {
        authService.sjekkLesetilgangMedFnr(fnr)
        return getOppfolgingStatusData(fnr)
    }

    private fun hentAktorIderMedOppfolging(fnr: Fnr?): List<AktorId> {
        authService.sjekkLesetilgangMedFnr(fnr)
        val aktorIder = authService.getAlleAktorIderOrThrow(fnr)
        return aktorIder
            .filter { aktorId -> !oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).isEmpty() }
    }

    fun hentHarFlereAktorIderMedOppfolging(fnr: Fnr?): Boolean {
        val harFlereAktorIdMedOppfolging = hentAktorIderMedOppfolging(fnr).size > 1

        if (harFlereAktorIdMedOppfolging) {
            brukerOppslagFlereOppfolgingAktorRepository.insertBrukerHvisNy(fnr)
        }

        return harFlereAktorIdMedOppfolging
    }


    fun harVeilederTilgangTilBrukersEnhet(fnr: Fnr): VeilederTilgang {
        authService.sjekkLesetilgangMedFnr(fnr)
        return arbeidsoppfolgingsKontorService.hentOppfolgingsEnhetId(fnr)
            ?.let{ enhetId -> authService.harTilgangTilEnhet(enhetId.get()) }
            ?.let{ harTilgang -> VeilederTilgang(harTilgang) }
            ?: VeilederTilgang(false)
    }

    fun hentOppfolgingsperioder(fnr: Fnr?): List<OppfolgingsperiodeEntity> {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return hentOppfolgingsperioder(aktorId)
    }

    fun hentOppfolgingsperioder(aktorId: AktorId): List<OppfolgingsperiodeEntity> {
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
    }

    fun oppfolgingData(fnr: Fnr): UnderOppfolgingDTO {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        authService.sjekkLesetilgangMedFnr(fnr)

        return getOppfolgingStatus(fnr)
            ?.let { oppfolgingsstatus ->
                val isUnderOppfolging = oppfolgingsstatus.underOppfolging
                val erManuell = manuellStatusService.erManuell(aktorId)
                UnderOppfolgingDTO(isUnderOppfolging, isUnderOppfolging && erManuell)
            } ?: UnderOppfolgingDTO(underOppfolging = false, erManuell = false)
    }

    fun erUnderOppfolgingNiva3(fnr: Fnr?): Boolean {
        val aktorId = authService.getAktorIdOrThrow(fnr)

        authService.sjekkTilgangTilPersonMedNiva3(aktorId)

        return erUnderOppfolging(aktorId)
    }

    fun erUnderOppfolging(fnr: Fnr?): Boolean {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return erUnderOppfolging(aktorId)
    }

    fun hentOppfolgingsperiode(uuid: String?): Optional<OppfolgingsperiodeEntity> {
        return oppfolgingsPeriodeRepository.hentOppfolgingsperiode(uuid)
    }

    fun hentOppfolgingsperioderMedKvp(aktorId: AktorId): List<OppfolgingsperiodeEntity> {
        val kvpPerioder = kvpRepository.hentKvpHistorikk(aktorId)
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            .populerKvpPerioder(kvpPerioder)
    }

    private fun hentGjeldendeKvpPeriode(oppfolgingEntity: OppfolgingEntity): KvpPeriodeEntity? {
        val gjeldendeKvpId = oppfolgingEntity.gjeldendeKvpId
        return if (gjeldendeKvpId != null && gjeldendeKvpId != 0L) {
            val kvpPeriode = kvpRepository.hentKvpPeriode(gjeldendeKvpId).orElse(null)
            if (kvpPeriode != null) {
                if (authService.harTilgangTilEnhet(kvpPeriode.enhet)) {
                    log.warn("Bruker hadde ikke tilgan til kvp-periode")
                    kvpPeriode
                } else {
                    // Hadde ikke tilgang til KVP-periode
                    null
                }
            } else {
                // Fant ikke kvp-periode, dette skal ikke skje
                log.error("Fant ikke KVP periode for id $gjeldendeKvpId")
                null
            }
        } else {
            null
        }
    }

    private fun hentGjeldendeMaal(oppfolgingEntity: OppfolgingEntity): MaalEntity? {
        val maalId = oppfolgingEntity.gjeldendeMaalId
        return if (maalId != null && maalId != 0L) {
            maalRepository.hentMaal(oppfolgingEntity.gjeldendeMaalId)
                .orElse(null)
                ?: run {
                    log.error("Fant ikke maal for id " + oppfolgingEntity.gjeldendeMaalId)
                    null
                }
        } else { null }
    }

    fun hentOppfolging(aktorId: AktorId): Optional<Oppfolging> {
        val oppfolgingEntity = oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null) ?: return Optional.empty()
        val kvpPeriode = hentGjeldendeKvpPeriode(oppfolgingEntity)
        val maalEntity = hentGjeldendeMaal(oppfolgingEntity)
        val manuellStatus = manuellStatusService.hentManuellStatus(aktorId)
        val oppfolgingsperioder = hentOppfolgingsperioderMedKvp(aktorId)

        val oppfolging = Oppfolging(
            oppfolgingEntity.aktorId!!,
            oppfolgingEntity.veilederId,
            oppfolgingEntity.underOppfolging,
            manuellStatus.orElse(null),
            maalEntity,
            oppfolgingsperioder,
            kvpPeriode
        )

        return Optional.of<Oppfolging>(oppfolging)
    }

    fun erUnderOppfolging(aktorId: AktorId): Boolean {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map(OppfolgingEntity::underOppfolging)
            .orElse(false)
    }

    fun harAktiveTiltaksdeltakelser(fnr: Fnr): Boolean {
        return tiltakshistorikkClient.harAktiveTiltaksdeltakelser(fnr.get())
    }

    private fun getOppfolgingStatus(fnr: Fnr?): OppfolgingEntity? {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        authService.sjekkLesetilgangMedAktorId(aktorId)
        return oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)
    }

    private fun getOppfolgingStatusData(fnr: Fnr): OppfolgingStatusData {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        val maybeOppfolging = hentOppfolging(aktorId)
        val erManuell = manuellStatusService.erManuell(aktorId)
        val digdirKontaktinfo = manuellStatusService.hentDigdirKontaktinfo(fnr)
        // TODO: Burde kanskje heller feile istedenfor å bruke Optional
        val maybeArenaOppfolging: Optional<VeilarbArenaOppfolgingsStatus> =
            arenaOppfolgingService.hentArenaOppfolgingsStatus(fnr)
        val harSkrivetilgangTilBruker = harVeilederTilgangTilKontorsperretEnhet(aktorId)

        val erInaktivIArena = maybeArenaOppfolging.map({ ao ->
            ArenaUtils.erIserv(
                EnumUtils.valueOf(Formidlingsgruppe::class.java, ao.formidlingsgruppe)
            )
        }).orElse(null)

        val maybeKanEnkeltReaktiveres = maybeArenaOppfolging
            .flatMap({ it -> Optional.ofNullable<Boolean?>(it.kanEnkeltReaktiveres) })

        val kanReaktiveres = maybeKanEnkeltReaktiveres
            .map({ kr ->
                maybeOppfolging.map(Oppfolging::underOppfolging).orElse(false) && kr
            })
            .orElse(null)

        val erSykmeldtMedArbeidsgiver = maybeArenaOppfolging
            .map({ ao ->
                ArenaUtils.erIARBSUtenOppfolging(
                    EnumUtils.valueOf(
                        Formidlingsgruppe::class.java,
                        ao.formidlingsgruppe
                    ), EnumUtils.valueOf(Kvalifiseringsgruppe::class.java, ao.servicegruppe)
                )
            })
            .orElse(null)

        val inaktiveringsDato = maybeArenaOppfolging
            .map<LocalDate?>(VeilarbArenaOppfolgingsStatus::inaktiveringsdato)
            .orElse(null)

        return OppfolgingStatusData(
            fnr.get(),
            aktorId.get(),
            maybeOppfolging.map<String?>(Oppfolging::veilederId).orElse(null),
            digdirKontaktinfo.reservert,
            digdirKontaktinfo.aktiv,
            erManuell || digdirKontaktinfo.reservert,
            maybeOppfolging.map(Oppfolging::underOppfolging).orElse(false),
            maybeOppfolging.map({ oppfolging -> oppfolging.gjeldendeKvp != null })
                .orElse(false) ?: false,
            maybeOppfolging.map({ oppfolging -> !oppfolging.underOppfolging })
                .orElse(true) ?: false,
            !erManuell && digdirKontaktinfo.kanVarsles,
            maybeOppfolging.map(Oppfolging::oppfolgingsperioder)
                .orElse(listOf()) ?: listOf(),
            mutableListOf(),  //KVP-perioder ble aldri satt før konvertering OppfolgingStatusData-klassen til Kotlin,
            harSkrivetilgangTilBruker,
            erInaktivIArena,
            kanReaktiveres,
            inaktiveringsDato,
            erSykmeldtMedArbeidsgiver,
            maybeArenaOppfolging.map<String?>(VeilarbArenaOppfolgingsStatus::servicegruppe).orElse(null),
            maybeArenaOppfolging.map<String?>(VeilarbArenaOppfolgingsStatus::formidlingsgruppe).orElse(null),
            maybeArenaOppfolging.map<String?>(VeilarbArenaOppfolgingsStatus::rettighetsgruppe).orElse(null),
            null
        )
    }

    fun harVeilederTilgangTilKontorsperretEnhet(aktorId: AktorId?): Boolean {
        val kvpId = kvpRepository.gjeldendeKvp(aktorId)
        val brukerErUtenKontorSperre = !kvpService.erUnderKvp(kvpId)
        return brukerErUtenKontorSperre || authService.harTilgangTilEnhet(
            kvpRepository.hentKvpPeriode(kvpId)
                .orElseThrow()
                .enhet
        )
    }

    private fun List<OppfolgingsperiodeEntity>.populerKvpPerioder(
        kvpPerioder: List<KvpPeriodeEntity>
    ): List<OppfolgingsperiodeEntity> {
        return this
            .map { periode ->
                val aktuelleKvpPerioder = kvpPerioder
                    .filter { kvp -> authService.harTilgangTilEnhetMedSperre(kvp.enhet) }
                    .filter { kvp -> erKvpIPeriode(kvp, periode) }
                periode.oppdaterMedKvpPerioder(aktuelleKvpPerioder)
            }
    }

    private fun erKvpIPeriode(kvp: KvpPeriodeEntity, periode: OppfolgingsperiodeEntity): Boolean {
        return kvpEtterStartenAvPeriode(kvp, periode)
                && kvpForSluttenAvPeriode(kvp, periode)
    }

    private fun kvpEtterStartenAvPeriode(kvp: KvpPeriodeEntity, periode: OppfolgingsperiodeEntity): Boolean {
        return !periode.startDato.isAfter(kvp.opprettetDato)
    }

    private fun kvpForSluttenAvPeriode(kvp: KvpPeriodeEntity, periode: OppfolgingsperiodeEntity): Boolean {
        return periode.sluttDato == null || !periode.sluttDato.isBefore(kvp.opprettetDato)
    }

    fun hentGjeldendeOppfolgingsperiode(fnr: Fnr): Optional<OppfolgingsperiodeEntity> {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return hentGjeldendeOppfolgingsperiode(aktorId)
    }

    fun hentGjeldendeOppfolgingsperiode(aktorId: AktorId): Optional<OppfolgingsperiodeEntity> {
        return oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
    }

    fun oppdaterArenaOppfolgingStatus(
        aktorId: AktorId,
        skalOppretteOppfolgingForst: Boolean,
        arenaOppfolging: LocalArenaOppfolging
    ) {
        oppfolgingsStatusRepository.oppdaterArenaOppfolgingStatus(
            aktorId,
            skalOppretteOppfolgingForst,
            arenaOppfolging
        )
    }
}