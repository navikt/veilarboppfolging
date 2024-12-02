package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import lombok.extern.slf4j.Slf4j
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.FantIkkeBrukerIArenaException
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand
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
            .map { arenaOppfolging -> ArenaOppfolgingTilstand.fraArenaOppfolging(arenaOppfolging) }
            .map { it.kanEnkeltReaktiveres }
    }

    // Bruker endepunktet i veilarbarena som henter direkte fra Arena
    private fun hentOppfolgingTilstandDirekteFraArena(fnr: Fnr): Optional<ArenaOppfolgingTilstand> {
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
            val oppfolgingTilstand: Optional<ArenaOppfolgingTilstand> = hentOppfolgingTilstandDirekteFraArena(fnr)

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
            .map { it.enhetId }
            .orElseGet { hentEnhetSynkrontFraVeilarbarena(fnr) }
    }

    fun hentArenaOppfolgingsEnhet(fnr: Fnr): Oppfolgingsenhet? {
        return hentArenaOppfolgingsEnhetId(fnr)
            ?.let { hentEnhet(it.get()) }
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

    fun getOppfolginsstatus(fnr: Fnr): GetOppfolginsstatusResult {
        val veilarbArenaOppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .let {
                if (it.isEmpty) { return GetOppfolginsstatusFailure(FantIkkeBrukerIArenaException()) }
                return@let it.get()
            }

        val veilederIdent = if (authService.erInternBruker()) hentTilordnetVeileder(fnr) else null

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
            return Oppfolgingsenhet(navn = enhetNavn, enhetId = enhetId)
        } catch (e: Exception) {
            log.warn("Fant ikke navn på enhet", e)
            return Oppfolgingsenhet("", enhetId)
        }
    }
}
