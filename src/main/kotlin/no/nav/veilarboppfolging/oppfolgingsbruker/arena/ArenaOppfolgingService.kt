package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import lombok.extern.slf4j.Slf4j
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.FantIkkeBrukerIArenaException
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.EnumUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Slf4j
@Service
open class ArenaOppfolgingService @Autowired constructor (
    // Bruker AktorregisterClient istedenfor authService for å unngå sirkulær avhengighet
    val aktorOppslagClient: AktorOppslagClient,
    val veilarbarenaClient: VeilarbarenaClient,
    val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    val authService: AuthService,
    val norg2Client: Norg2Client,
    val veilederTilordningerRepository: VeilederTilordningerRepository,
    val historikkRepository: OppfolgingsenhetHistorikkRepository
) {
    private val log = LoggerFactory.getLogger(ArenaOppfolgingService::class.java)

    fun kanEnkeltReaktiveres(fnr: Fnr): Optional<Boolean> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
            .map { it.kanEnkeltReaktiveres }
    }

    // Bruker endepunktet i veilarbarena som henter direkte fra Arena
    private fun hentOppfolgingTilstandDirekteFraArena(fnr: Fnr): Optional<ArenaOppfolgingTilstand> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
            .map { arenaOppfolging -> ArenaOppfolgingTilstand.fraArenaOppfolging(arenaOppfolging) }
    }

    private fun erUnderOppfolgingIVeilarbarena(maybeArenaOppfolging: Optional<ArenaOppfolgingTilstand>): Boolean {
        return maybeArenaOppfolging
            .map { oppfolging ->
                ArenaUtils.erUnderOppfolging(
                    EnumUtils.valueOf<Formidlingsgruppe>(
                        Formidlingsgruppe::class.java,
                        oppfolging.getFormidlingsgruppe()),
                    EnumUtils.valueOf<Kvalifiseringsgruppe>(
                        Kvalifiseringsgruppe::class.java,
                        oppfolging.getServicegruppe())
                )
            }.orElse(false)
    }

    fun hentArenaOppfolgingsbruker(fnr: Fnr): Optional<VeilarbArenaOppfolgingsBruker> {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
    }

    /**
     *  Brukes kun hvis man trenger [VeilarbArenaOppfolgingsStatus.kanEnkeltReaktiveres]
     *  @see no.nav.veilarboppfolging.client.veilarbarenaVeilarbArenaOppfolgingsStatus#kanEnkeltReaktiveres
     *  */
    fun hentArenaOppfolgingsStatus(fnr: Fnr): Optional<VeilarbArenaOppfolgingsStatus> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
    }

    /* Bare den delen av oppfølgingstilstand hvor arena er kilden */
    fun hentArenaOppfolgingTilstand(fnr: Fnr): Optional<ArenaOppfolgingTilstand> {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        return oppfolging.flatMap { it.localArenaOppfolging }
            .map { ArenaOppfolgingTilstand.fraLocalArenaOppfolging(it) }
            .or { // Fallback til synkront endepunkt
                veilarbarenaClient.hentOppfolgingsbruker(fnr)
                    .map { ArenaOppfolgingTilstand.fraArenaBruker(it) } // Oppfølgingsbruker endepunkt
            }
    }


    private fun hentTilordnetVeileder(fnr: Fnr): String? {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        secureLog.info("Henter tilordning for bruker med aktørId {}", aktorId)
        return veilederTilordningerRepository.hentTilordningForAktoer(aktorId)
    }


    private fun hentEnhetSynkrontFraVeilarbarena(fnr: Fnr): EnhetId? {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .map { it.nav_kontor }
            .map { EnhetId(it) }.orElse(null)
    }

    /* Det finnes Noen få brukere som ikke har enhet */
    fun hentArenaOppfolgingsEnhetId(fnr: Fnr): EnhetId? {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .flatMap { it.localArenaOppfolging.map { it.oppfolgingsenhet } }
            .orElseGet { hentEnhetSynkrontFraVeilarbarena(fnr) }
    }

    fun hentArenaOppfolgingsEnhet(fnr: Fnr): Oppfolgingsenhet? {
        return hentArenaOppfolgingsEnhetId(fnr)
            ?.let { hentEnhet(it) }
    }

    data class IservDatoOgFormidlingsGruppe(
        val iservDato: ZonedDateTime?,
        val formidlingsGruppe: Formidlingsgruppe?
    )
    fun hentIservDatoOgFormidlingsGruppe(fnr: Fnr): IservDatoOgFormidlingsGruppe? {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .map { oppfolgingsbruker ->
                IservDatoOgFormidlingsGruppe(
                    oppfolgingsbruker.iserv_fra_dato,
                    oppfolgingsbruker.formidlingsgruppekode?.let { Formidlingsgruppe.valueOf(it) }
                )
            }.orElse(null)
    }

    /* Egentlig ikke oppfolgingsstatus men oppfølgingsbruker men endepunktet heter /oppfolgingsstatus */
    fun hentArenaOppfolginsstatus(fnr: Fnr): GetOppfolginsstatusResult {
        val aktorId = authService.getAktorIdOrThrow(fnr)

        val lokaltLagretOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        val localArenaOppfolging = lokaltLagretOppfolging.flatMap { it.localArenaOppfolging }

        val oppfolgingsData: OppfolgingsData = when {
            (localArenaOppfolging.isPresent) -> {
                OppfolgingsData(
                    localArenaOppfolging.get().oppfolgingsenhet?.get()?.let { EnhetId(it)  },
                    localArenaOppfolging.get().kvalifiseringsgruppe,
                    localArenaOppfolging.get().formidlingsgruppe,
                    localArenaOppfolging.get().hovedmaal)
            }
            else -> {
                val veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                    .let {
                        if (it.isEmpty) { return GetOppfolginsstatusFailure(FantIkkeBrukerIArenaException()) }
                        return@let it.get()
                    }
                OppfolgingsData(
                    veilarbArenaOppfolging.nav_kontor?.let { EnhetId.of(it) },
                    Kvalifiseringsgruppe.valueOf(veilarbArenaOppfolging.kvalifiseringsgruppekode),
                    Formidlingsgruppe.valueOf(veilarbArenaOppfolging.formidlingsgruppekode),
                    Hovedmaal.valueOf(veilarbArenaOppfolging.hovedmaalkode)
                )
            }
        }

        val veilederIdent = if (authService.erInternBruker()) lokaltLagretOppfolging.map { it.veilederId  }.orElse(null) else null
        return GetOppfolginsstatusSuccess(
            OppfolgingEnhetMedVeilederResponse(
                oppfolgingsenhet = oppfolgingsData.enhetId?.let { hentEnhet(it) } ,
                veilederId = veilederIdent,
                formidlingsgruppe = oppfolgingsData.formidlingsgruppe.name,
                servicegruppe = oppfolgingsData.kvalifiseringsgruppe.name,
                hovedmaalkode = oppfolgingsData.hovedmaal?.name,
            )
        )
    }

    private fun hentEnhet(enhetId: EnhetId?): Oppfolgingsenhet? {
        if (enhetId == null) return null
        try {
            val enhetNavn = norg2Client.hentEnhet(enhetId.get()).getNavn()
            return Oppfolgingsenhet(navn = enhetNavn, enhetId = enhetId.get())
        } catch (e: Exception) {
            log.warn("Fant ikke navn på enhet", e)
            return Oppfolgingsenhet("", enhetId.get())
        }
    }
}

data class OppfolgingsData(
    val enhetId: EnhetId?,
    var kvalifiseringsgruppe: Kvalifiseringsgruppe,
    var formidlingsgruppe: Formidlingsgruppe,
    var hovedmaal: Hovedmaal?,
)