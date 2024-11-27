package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import lombok.extern.slf4j.Slf4j
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.FantIkkeBrukerIArenaException
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.utils.ArenaUtils
import no.nav.veilarboppfolging.utils.EnumUtils
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Supplier

@Slf4j
@Service
class ArenaOppfolgingService (
    // Bruker AktorregisterClient istedenfor authService for å unngå sirkulær avhengighet
    private val aktorOppslagClient: AktorOppslagClient,
    private val veilarbarenaClient: VeilarbarenaClient,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val authService: AuthService,
    private val norg2Client: Norg2Client,
    private val veilederTilordningerRepository: VeilederTilordningerRepository
) {
    private val log = LoggerFactory.getLogger(ArenaOppfolgingService::class.java)

    // Bruker endepunktet i veilarbarena som henter fra database som er synket med Arena (har et delay på et par min)
    fun hentOppfolgingFraVeilarbarena(fnr: Fnr): Optional<VeilarbArenaOppfolging>? {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
    }

    // Bruker endepunktet i veilarbarena som henter direkte fra Arena
    fun hentOppfolgingTilstandDirekteFraArena(fnr: Fnr): Optional<ArenaOppfolgingTilstand> {
        return veilarbarenaClient.getArenaOppfolgingsstatus(fnr)
            .map { arenaOppfolging -> ArenaOppfolgingTilstand.fraArenaOppfolging(arenaOppfolging) }
    }

    fun hentOppfolgingTilstand(fnr: Fnr): Optional<ArenaOppfolgingTilstand> {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)

        val maybeArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .map { veilarbArenaOppfolging -> ArenaOppfolgingTilstand.fraArenaBruker(veilarbArenaOppfolging) }

        // TODO: Kan hente erUnderOppfolging gjennom OppfolgingService
        val oppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        val erUnderOppfolging = oppfolging.map { it.isUnderOppfolging }.orElse(false)

        val erUnderOppfolgingIVeilarbarena = maybeArenaOppfolging
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

        if (erUnderOppfolgingIVeilarbarena != erUnderOppfolging) {
            val oppfolgingTilstand = hentOppfolgingTilstandDirekteFraArena(fnr)

            maybeArenaOppfolging.ifPresent { arenaOppfolging ->
                log.info(
                    ("Differanse mellom oppfolging fra veilarbarena og direkte fra Arena."
                            + " veilarbarena.formidlingsgruppe={} veilarbarena.servicegruppe={}"
                            + " arena.formidlingsgruppe={} arena.servicegruppe={}"),
                    arenaOppfolging.getFormidlingsgruppe(),
                    arenaOppfolging.getServicegruppe(),
                    oppfolgingTilstand.map { it?.getFormidlingsgruppe() }.orElse(null),
                    oppfolgingTilstand.map { it.getServicegruppe() }.orElse(null)
                )
            }

            return oppfolgingTilstand
        }

        return maybeArenaOppfolging
    }

    private fun hentTilordnetVeileder(fnr: Fnr): String {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        secureLog.info("Henter tilordning for bruker med aktørId {}", aktorId)
        return veilederTilordningerRepository.hentTilordningForAktoer(aktorId)
    }

    fun getOppfolginsstatus(fnr: Fnr): GetOppfolginsstatusResult {
        val veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .let {
                if (it.isEmpty) { return GetOppfolginsstatusFailure(FantIkkeBrukerIArenaException()) }
                return@let it.get()
            }

        val veilederIdent = if (authService.erInternBruker()) hentTilordnetVeileder(fnr) else ""

        return GetOppfolginsstatusSuccess(
            OppfolgingEnhetMedVeilederResponse(
                oppfolgingsenhet = hentEnhet(veilarbArenaOppfolging.getNav_kontor()),
                veilederId = veilederIdent,
                formidlingsgruppe = veilarbArenaOppfolging.getFormidlingsgruppekode(),
                servicegruppe = veilarbArenaOppfolging.getKvalifiseringsgruppekode(),
                hovedmaalkode = veilarbArenaOppfolging.getHovedmaalkode(),
            )
        )
    }

    private fun hentEnhet(enhetId: String): Oppfolgingsenhet {
        try {
            val enhetNavn = norg2Client.hentEnhet(enhetId).getNavn()
            return Oppfolgingsenhet(enhetNavn, enhetId)
        } catch (e: Exception) {
            log.warn("Fant ikke navn på enhet", e)
            return Oppfolgingsenhet("", enhetId)
        }
    }
}
