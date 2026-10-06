package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO
import no.nav.veilarboppfolging.controller.response.VeilederTilgang
import no.nav.veilarboppfolging.domain.Oppfolging
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class OppfolgingService @Autowired constructor(
    private val kvpService: KvpService,
    private val authService: AuthService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,  // TODO: Når vi får splittet servicenen bedre så skal det ikke være behov for å bruke @Lazy
    @param:Lazy private val manuellStatusService: ManuellStatusService,
    private val kvpRepository: KvpRepository,
    private val maalRepository: MaalRepository,
    private val brukerOppslagFlereOppfolgingAktorRepository: BrukerOppslagFlereOppfolgingAktorRepository,
    private val arbeidsoppfolgingsKontorService: ArbeidsoppfolgingsKontorService,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val aktorOppslagClient: AktorOppslagClient
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

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
        val tilgangTilBruker = authService.evaluerNavAnsattTilgangTilBruker(fnr, TilgangType.LESE)
        if (tilgangTilBruker.isDeny) return VeilederTilgang(false)

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
        val identer = aktorOppslagClient.hentIdenter(fnr)
            .let { it.historiskeFnr + listOf(it.fnr) }
        return tiltakshistorikkClient.harAktiveTiltaksdeltakelser(identer)
    }

    private fun getOppfolgingStatus(fnr: Fnr?): OppfolgingEntity? {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        authService.sjekkLesetilgangMedAktorId(aktorId)
        return oppfolgingsStatusRepository.hentOppfolging(aktorId).orElse(null)
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