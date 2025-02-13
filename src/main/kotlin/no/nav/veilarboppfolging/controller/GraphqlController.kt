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
import no.nav.veilarboppfolging.ForbiddenException
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

enum class KanStarteOppfolging {
    JA,
    NEI_ALLEREDE_UNDER_OPPFOLGING,
    NEI_IKKE_TILGANG_KODE_7,
    NEI_IKKE_TILGANG_KODE_6,
    NEI_IKKE_TILGANG_SKJERMING,
    NEI_IKKE_TILGANG_ENHET,
    NEI_IKKE_TILGANG_MODIA
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
    private val poaoTilgangClient: PoaoTilgangClient
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

    @SchemaMapping(typeName="OppfolgingsEnhetsInfo", field="enhet")
    fun kanStarteOppfolging(oppfolgingDto: OppfolgingDto): KanStarteOppfolging? {
        if (oppfolgingDto.norskIdent == null) throw InternFeil("Fant ikke fnr å sjekke tilgang mot i kanStarteOppfolging")
        if (oppfolgingDto.erUnderOppfolging) return KanStarteOppfolging.NEI_ALLEREDE_UNDER_OPPFOLGING
        try {
            val decision = authService.evaluerNavAnsattTilagngTilBruker(Fnr.of(oppfolgingDto.norskIdent), TilgangType.LESE)
            return when (decision) {
                is Decision.Deny -> decision.tryToFindDenyReason()
                Decision.Permit -> KanStarteOppfolging.JA
            }
        } catch (e: ForbiddenException) {
            logger.error("Sjekking av tilgang for nav-ansatt feilet", e)
            throw InternFeil("Sjekking av tilgang for nav-ansatt feilet")
        }
    }

    fun hentDefaultEnhetFraNorg(fnr: Fnr): Pair<EnhetId, KildeDto>? {
        val tilgangsattributterResponse = poaoTilgangClient.hentTilgangsAttributter(fnr.get())
        if (tilgangsattributterResponse.isFailure) throw PoaoTilgangError(tilgangsattributterResponse.exception!!)
        val tilgangsAttributter = tilgangsattributterResponse.getOrThrow()
        return tilgangsAttributter.kontor?.let { EnhetId.of(it) to KildeDto.NORG }
    }
}

fun Decision.Deny.tryToFindDenyReason(): KanStarteOppfolging {
    if (this.reason != "MANGLER_TILGANG_TIL_AD_GRUPPE") return KanStarteOppfolging.NEI_IKKE_TILGANG_ENHET
    return when {
        this.message.contains(AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE) -> return KanStarteOppfolging.NEI_IKKE_TILGANG_KODE_6
        this.message.contains(AdGruppeNavn.FORTROLIG_ADRESSE) -> return KanStarteOppfolging.NEI_IKKE_TILGANG_KODE_7
        this.message.contains(AdGruppeNavn.EGNE_ANSATTE) -> return KanStarteOppfolging.NEI_IKKE_TILGANG_SKJERMING
        this.message.contains(AdGruppeNavn.MODIA_GENERELL) -> return KanStarteOppfolging.NEI_IKKE_TILGANG_MODIA
        this.message.contains(AdGruppeNavn.MODIA_OPPFOLGING) -> return KanStarteOppfolging.NEI_IKKE_TILGANG_MODIA
        else -> KanStarteOppfolging.NEI_IKKE_TILGANG_ENHET
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