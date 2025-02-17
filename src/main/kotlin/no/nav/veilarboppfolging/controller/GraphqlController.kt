package no.nav.veilarboppfolging.controller

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NorskIdent
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarboppfolging.client.pdl.Folkeregisterpersonstatus
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException

data class OppfolgingsEnhetQueryDto(
    val enhet: EnhetDto?, // Nullable because graphql
    val fnr: String // Only used to pass fnr to "sub-queries"
)

data class EnhetDto(
    val id: String,
    val navn: String,
    val kilde: KildeDto
)

enum class KildeDto {
    ARENA,
    NORG
}

data class VeilederTilgangDto(
    val harTilgang: Boolean?,
    val tilgang: TilgangResultat?
)

enum class TilgangResultat {
    HAR_TILGANG,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA
}

enum class KanStarteOppfolging {
    JA,
    ALLEREDE_UNDER_OPPFOLGING,
    DOD,
    IKKE_LOVLIG_OPPHOLD,
    UKJENT_STATUS_FOLKEREGISTERET,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA;

    infix fun and(kanStarteOppfolging: Lazy<KanStarteOppfolging>): KanStarteOppfolging {
        if (this != JA) return this
        return kanStarteOppfolging.value
    }
}

data class OppfolgingDto(
    val erUnderOppfolging: Boolean,
    val kanStarteOppfolging: KanStarteOppfolging?,
    val norskIdent: NorskIdent? = null
)

@Controller
class GraphqlController(
    private val enhetRepository: EnhetRepository,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val norg2Client: Norg2Client,
    private val aktorOppslagClient: AktorOppslagClient,
    private val authService: AuthService,
    private val poaoTilgangClient: PoaoTilgangClient,
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient
) {
    private val logger = LoggerFactory.getLogger(GraphqlController::class.java)

    init {
        logger.info("Started GraphqlController")
    }

    @QueryMapping
    fun oppfolgingsEnhet(@Argument fnr: String?): OppfolgingsEnhetQueryDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return OppfolgingsEnhetQueryDto(fnr = fnr, enhet = null)
    }

    @QueryMapping
    fun oppfolging(@Argument fnr: String?): OppfolgingDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(fnr)) as AktorId?
        if (aktorId == null) throw FantIkkeAktorIdForFnrError()
        val erUnderOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map { it.isUnderOppfolging }.orElse(false)
        return OppfolgingDto(erUnderOppfolging, null, fnr)
    }

    @QueryMapping
    fun veilederLeseTilgangModia(@Argument fnr: String?): VeilederTilgangDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return evaluerTilgang(fnr)
            .let { VeilederTilgangDto(
                harTilgang = it == TilgangResultat.HAR_TILGANG,
                tilgang =  it)
            }
    }

    private fun evaluerTilgang(fnr: String): TilgangResultat {
        val decision = authService.evaluerNavAnsattTilagngTilBruker(Fnr.of(fnr), TilgangType.LESE)
        return when (decision) {
            is Decision.Deny -> decision.tryToFindDenyReason()
            Decision.Permit -> TilgangResultat.HAR_TILGANG
        }
    }

    private fun kanStarteOppfolgingMtpFregStatus(fnr: Fnr): KanStarteOppfolging {
        return pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr).toKanStarteOppfolging()
    }

    @SchemaMapping(typeName="OppfolgingsEnhetsInfo", field="enhet")
    fun arenaOppfolgingsEnhet(oppfolgingsEnhet: OppfolgingsEnhetQueryDto): EnhetDto? {
        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(oppfolgingsEnhet.fnr))
        val arenaEnhet = enhetRepository.hentEnhet(aktorId)
        return when {
            arenaEnhet == null -> hentDefaultEnhetFraNorg(Fnr.of(oppfolgingsEnhet.fnr))
            else -> arenaEnhet to KildeDto.ARENA
        }?.let { (enhetsNr, kilde) ->
            val enhet = norg2Client.hentEnhet(enhetsNr.get())
            EnhetDto(
                id = enhetsNr.get(),
                navn = enhet.navn,
                kilde = kilde
            )
        }
    }

    @SchemaMapping(typeName="OppfolgingDto", field="kanStarteOppfolging")
    fun kanStarteOppfolging(oppfolgingDto: OppfolgingDto): KanStarteOppfolging? {
        if (oppfolgingDto.norskIdent == null) throw InternFeil("Fant ikke fnr å sjekke tilgang mot i kanStarteOppfolging")
        val gyldigOppfolging = if (oppfolgingDto.erUnderOppfolging) KanStarteOppfolging.ALLEREDE_UNDER_OPPFOLGING else KanStarteOppfolging.JA
        val gyldigTilgang = lazy { evaluerTilgang(oppfolgingDto.norskIdent).toKanStarteOppfolging() }
        val gyldigFregStatus = lazy { kanStarteOppfolgingMtpFregStatus(Fnr.of(oppfolgingDto.norskIdent)) }
        return gyldigOppfolging and gyldigTilgang and gyldigFregStatus
    }

    fun hentDefaultEnhetFraNorg(fnr: Fnr): Pair<EnhetId, KildeDto>? {
        val tilgangsattributterResponse = poaoTilgangClient.hentTilgangsAttributter(fnr.get())
        if (tilgangsattributterResponse.isFailure) throw PoaoTilgangError(tilgangsattributterResponse.exception!!)
        val tilgangsAttributter = tilgangsattributterResponse.getOrThrow()
        return tilgangsAttributter.kontor?.let { EnhetId.of(it) to KildeDto.NORG }
    }
}

fun Decision.Deny.tryToFindDenyReason(): TilgangResultat {
    if (this.reason != "MANGLER_TILGANG_TIL_AD_GRUPPE") return TilgangResultat.IKKE_TILGANG_ENHET
    return when {
        this.message.contains(AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE) -> return TilgangResultat.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
        this.message.contains(AdGruppeNavn.FORTROLIG_ADRESSE) -> return TilgangResultat.IKKE_TILGANG_FORTROLIG_ADRESSE
        this.message.contains(AdGruppeNavn.EGNE_ANSATTE) -> return TilgangResultat.IKKE_TILGANG_EGNE_ANSATTE
        this.message.contains(AdGruppeNavn.MODIA_GENERELL) -> return TilgangResultat.IKKE_TILGANG_MODIA
        this.message.contains(AdGruppeNavn.MODIA_OPPFOLGING) -> return TilgangResultat.IKKE_TILGANG_MODIA
        else -> TilgangResultat.IKKE_TILGANG_ENHET
    }
}

fun TilgangResultat.toKanStarteOppfolging(): KanStarteOppfolging {
    return when (this) {
        TilgangResultat.HAR_TILGANG -> KanStarteOppfolging.JA
        TilgangResultat.IKKE_TILGANG_FORTROLIG_ADRESSE -> KanStarteOppfolging.IKKE_TILGANG_FORTROLIG_ADRESSE
        TilgangResultat.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE -> KanStarteOppfolging.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
        TilgangResultat.IKKE_TILGANG_EGNE_ANSATTE -> KanStarteOppfolging.IKKE_TILGANG_EGNE_ANSATTE
        TilgangResultat.IKKE_TILGANG_ENHET -> KanStarteOppfolging.IKKE_TILGANG_ENHET
        TilgangResultat.IKKE_TILGANG_MODIA -> KanStarteOppfolging.IKKE_TILGANG_MODIA
    }
}

fun ForenkletFolkeregisterStatus.toKanStarteOppfolging(): KanStarteOppfolging {
    return when (this) {
        ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
        ForenkletFolkeregisterStatus.dNummer -> KanStarteOppfolging.JA
        ForenkletFolkeregisterStatus.opphoert,
        ForenkletFolkeregisterStatus.ikkeBosatt,
        ForenkletFolkeregisterStatus.forsvunnet-> KanStarteOppfolging.IKKE_LOVLIG_OPPHOLD
        ForenkletFolkeregisterStatus.doedIFolkeregisteret -> KanStarteOppfolging.DOD
        ForenkletFolkeregisterStatus.ukjent -> KanStarteOppfolging.UKJENT_STATUS_FOLKEREGISTERET
    }
}

object AdGruppeNavn {
    const val STRENGT_FORTROLIG_ADRESSE     = "0000-GA-Strengt_Fortrolig_Adresse"
    const val FORTROLIG_ADRESSE     		= "0000-GA-Fortrolig_Adresse"
    const val EGNE_ANSATTE               	= "0000-GA-Egne_ansatte"
    /* AKA modia generell tilgang, en av disse trengs for lese-tilgang */
    const val MODIA_OPPFOLGING              = "0000-GA-Modia-Oppfolging"
    const val MODIA_GENERELL                = "0000-GA-BD06_ModiaGenerellTilgang"
}