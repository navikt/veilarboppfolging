package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import java.time.LocalDate
import java.util.Optional
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
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolginsBrukerOppslagResult
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaResult
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Slf4j
@Service
class ArenaOppfolgingService @Autowired constructor(
    // Bruker AktorregisterClient istedenfor authService for å unngå sirkulær avhengighet
    private val aktorOppslagClient: AktorOppslagClient,
    private val veilarbarenaClient: VeilarbarenaClient,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val authService: AuthService,
    private val norg2Client: Norg2Client,
) {
    private val log = LoggerFactory.getLogger(ArenaOppfolgingService::class.java)

    fun kanEnkeltReaktiveres(fnr: Fnr): Optional<Boolean> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
            .flatMap { Optional.ofNullable(it.kanEnkeltReaktiveres) }
    }

    fun brukerErIservIArena(fnr: Fnr): Boolean {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
            .map { it.formidlingsgruppe == Formidlingsgruppe.ISERV.name }
            .orElse(false) // Nei hvis bruker ikke finnes i arena eller ikke får svar fra arena
    }

    /**
     *  Brukes kun hvis man trenger [VeilarbArenaOppfolgingsStatus.kanEnkeltReaktiveres] , dette feltet kommer ikke på topic og kan derfor ikke caches i lokalt
     *  @see no.nav.veilarboppfolging.client.veilarbarenaVeilarbArenaOppfolgingsStatus#kanEnkeltReaktiveres
     *  */
    fun hentArenaOppfolgingsStatus(fnr: Fnr): Optional<VeilarbArenaOppfolgingsStatus> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
    }

    /**
     *  Henter arena-oppfolgingstilstand lokalt med fallback til arena /oppfolgingsbruker.
     *  Eksponerer bare felter som er felles for /oppfolgingsbruker og /oppfolgingsstatus:
     *  - ikke [VeilarbArenaOppfolgingsStatus.kanEnkeltReaktiveres]
     *  - ikke [VeilarbArenaOppfolgingsBruker.hovedmaalkode]
     *  */
    fun hentArenaOppfolgingTilstand(fnr: Fnr): ArenaOppfolgingTilstandOppslagResult {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        val localArenaOppfolging = oppfolging.getOrNull()?.localArenaOppfolging?.getOrNull()
        if (localArenaOppfolging != null) {
            return ArenaOppfolgingTilstandOppslagResult.Success(ArenaOppfolgingTilstand.fraLocalArenaOppfolging(localArenaOppfolging))
        }
        val result = veilarbarenaClient.hentOppfolgingsbruker(fnr)
        return when (result) {
            is ArenaOppfolginsBrukerOppslagResult.Fail -> ArenaOppfolgingTilstandOppslagResult.Fail(
                "Feil ved henting av oppfolgingsbruker fra arena",
                result.reason
            )
            is ArenaOppfolginsBrukerOppslagResult.NotFound -> ArenaOppfolgingTilstandOppslagResult.NotFound()
            is ArenaOppfolginsBrukerOppslagResult.Success -> ArenaOppfolgingTilstandOppslagResult.Success(
                ArenaOppfolgingTilstand.fraArenaBruker(result.oppfolgingsBruker)
            )
        }
    }

    fun hentIservDatoOgFormidlingsGruppe(fnr: Fnr): IservDatoOgFormidlingsGruppe? {
        val arenaOppfolingTilstandResult = hentArenaOppfolgingTilstand(fnr)
        return when(arenaOppfolingTilstandResult) {
            is ArenaOppfolgingTilstandOppslagResult.Fail, is ArenaOppfolgingTilstandOppslagResult.NotFound  -> null
            is ArenaOppfolgingTilstandOppslagResult.Success -> {
                val iservDato = arenaOppfolingTilstandResult.arenaOppfolingTilstand.inaktiveringsdato
                val formidlingsgruppe = arenaOppfolingTilstandResult.arenaOppfolingTilstand.formidlingsgruppe?.let {
                    Formidlingsgruppe.valueOf(it)
                }
                IservDatoOgFormidlingsGruppe(iservDato, formidlingsgruppe)
            }
        }
    }

    /* Egentlig ikke oppfolgingsstatus men oppfølgingsbruker men endepunktet heter /oppfolgingsstatus */
    fun hentArenaOppfolginsstatusMedHovedmaal(fnr: Fnr): GetOppfolginsstatusResult {
        val aktorId = authService.getAktorIdOrThrow(fnr)

        val lokaltLagretOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        val localArenaOppfolging = lokaltLagretOppfolging.flatMap { it.localArenaOppfolging }
        val oppfolgingsEnhet: EnhetId? = lokaltLagretOppfolging.map { it.oppfolgingsEnhet }.orElse(null)

        val oppfolgingsData: OppfolgingsData = when {
            (localArenaOppfolging.isPresent) -> {
                OppfolgingsData(
                    localArenaOppfolging.get().kvalifiseringsgruppe,
                    localArenaOppfolging.get().formidlingsgruppe,
                    localArenaOppfolging.get().hovedmaal
                )
            }
            else -> {
                val oppfolgingsbrukerOppslag = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                val veilarbArenaOppfolging = when(oppfolgingsbrukerOppslag) {
                    is ArenaOppfolginsBrukerOppslagResult.Success -> oppfolgingsbrukerOppslag.oppfolgingsBruker
                    is ArenaOppfolginsBrukerOppslagResult.NotFound, is ArenaOppfolginsBrukerOppslagResult.Fail -> return GetOppfolginsstatusFailure(FantIkkeBrukerIArenaException())
                }
                OppfolgingsData(
                    Kvalifiseringsgruppe.valueOf(veilarbArenaOppfolging.kvalifiseringsgruppekode),
                    Formidlingsgruppe.valueOf(veilarbArenaOppfolging.formidlingsgruppekode),
                    veilarbArenaOppfolging.hovedmaalkode?.let { Hovedmaal.valueOf(it) }
                )
            }
        }

        val veilederIdent =
            if (authService.erInternBruker()) lokaltLagretOppfolging.map { it.veilederId }.orElse(null) else null
        return GetOppfolginsstatusSuccess(
            OppfolgingEnhetMedVeilederResponse(
                oppfolgingsenhet = oppfolgingsEnhet?.let { hentEnhet(it) },
                veilederId = veilederIdent,
                formidlingsgruppe = oppfolgingsData.formidlingsgruppe.name,
                servicegruppe = oppfolgingsData.kvalifiseringsgruppe.name,
                hovedmaalkode = oppfolgingsData.hovedmaal?.name,
            )
        )
    }

    fun registrerIkkeArbeidssoker(fnr: Fnr): RegistrerIArenaResult {
        return veilarbarenaClient.registrerIkkeArbeidsoker(fnr)
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

sealed class ArenaOppfolgingTilstandOppslagResult {
    class Success(val arenaOppfolingTilstand: ArenaOppfolgingTilstand) : ArenaOppfolgingTilstandOppslagResult()
    class NotFound : ArenaOppfolgingTilstandOppslagResult()
    class Fail(val message: String, val reason: Throwable) : ArenaOppfolgingTilstandOppslagResult()
}

data class OppfolgingsData(
    var kvalifiseringsgruppe: Kvalifiseringsgruppe,
    var formidlingsgruppe: Formidlingsgruppe,
    var hovedmaal: Hovedmaal?
)

data class IservDatoOgFormidlingsGruppe(
    val iservDato: LocalDate?,
    val formidlingsGruppe: Formidlingsgruppe?
)