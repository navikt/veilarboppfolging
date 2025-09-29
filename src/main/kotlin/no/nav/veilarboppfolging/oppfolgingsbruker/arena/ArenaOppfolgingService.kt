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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@Slf4j
@Service
class ArenaOppfolgingService @Autowired constructor (
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
            .map {  it.formidlingsgruppe == Formidlingsgruppe.ISERV.name }
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

    /*
    * Oppfolgingsenhet i arena inkl navn, det finnes Noen få brukere som ikke har enhet.
    * Det gjøres oppslag i norg2 for å finne navn
    * */
    fun hentArenaOppfolgingsEnhet(fnr: Fnr): Oppfolgingsenhet? {
        return hentArenaOppfolgingsEnhetId(fnr)
            ?.let { hentEnhet(it) }
    }

    /* Bare enhetsId, den blir cached lokalt så man trenger ikke alltid hente den synkront */
    fun hentArenaOppfolgingsEnhetId(fnr: Fnr): EnhetId? {
        return hentArenaOppfolgingsEnhetMedSistEndret(fnr)?.enhet
    }

    /* Bare enhetsId, den blir cached lokalt så man trenger ikke alltid hente den synkront */
    fun hentArenaOppfolgingsEnhetMedSistEndret(fnr: Fnr): SisteOppfolgingsEnhet? {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .flatMap { oppfolging ->
                val sistEndretArena = oppfolging.oppdatert
                val enhet = oppfolging.localArenaOppfolging.flatMap { Optional.ofNullable(it.oppfolgingsenhet) }
                enhet.map { SisteOppfolgingsEnhet(it, sistEndretArena) }
            }
            .orElseGet { hentEnhetSynkrontFraVeilarbarena(fnr) }
    }

    private fun hentEnhetSynkrontFraVeilarbarena(fnr: Fnr): SisteOppfolgingsEnhet? {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
            .map { it.nav_kontor }
            /* Vi får ikke noe oppdatert-timestamp fra arena men siden vi nylig hentet
            * datene vet vi at de er ferske og kan derfor sist endret til nå (minus litt
            * slack i tilfelle arena enhet ble oppdatert fra vi gjorde requesten) */
            .map { SisteOppfolgingsEnhet(EnhetId(it), ZonedDateTime.now().minusSeconds(5)) }
            .orElse(null)
    }

    fun hentIservDatoOgFormidlingsGruppe(fnr: Fnr): IservDatoOgFormidlingsGruppe? {
        return hentArenaOppfolgingTilstand(fnr)
            .map { oppfolgingsbruker ->
                val iservDato = oppfolgingsbruker.inaktiveringsdato
                val formidlingsgruppe = oppfolgingsbruker.formidlingsgruppe?.let { Formidlingsgruppe.valueOf(it) }
                IservDatoOgFormidlingsGruppe(iservDato, formidlingsgruppe)
            }.orElse(null)
    }

    /* Egentlig ikke oppfolgingsstatus men oppfølgingsbruker men endepunktet heter /oppfolgingsstatus */
    fun hentArenaOppfolginsstatusMedHovedmaal(fnr: Fnr): GetOppfolginsstatusResult {
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
                    veilarbArenaOppfolging.hovedmaalkode?.let { Hovedmaal.valueOf(it) }
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

data class OppfolgingsData(
    val enhetId: EnhetId?,
    var kvalifiseringsgruppe: Kvalifiseringsgruppe,
    var formidlingsgruppe: Formidlingsgruppe,
    var hovedmaal: Hovedmaal?
)

data class IservDatoOgFormidlingsGruppe(
    val iservDato: LocalDate?,
    val formidlingsGruppe: Formidlingsgruppe?
)

data class SisteOppfolgingsEnhet(
    val enhet: EnhetId,
    val sistEndretArena: ZonedDateTime,
)