package no.nav.veilarboppfolging.controller

import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NorskIdent
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.kafka.dto.StartetBegrunnelseDTO
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING.oppfolgingSjekk
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FregStatusSjekkResultat
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OPPFOLGING_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.toKanStarteOppfolging
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

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


data class OppfolgingDto(
    val erUnderOppfolging: Boolean,
    val kanStarteOppfolging: KanStarteOppfolgingDto?,
    val norskIdent: NorskIdent? = null,
)

data class OppfolgingsperiodeDto(
    val startTidspunkt: String,
    val sluttTidspunkt: String?,
    val id: String,
    val startetBegrunnelse: String
)

@Controller
class GraphqlController(
    private val enhetRepository: EnhetRepository,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val norg2Client: Norg2Client,
    private val aktorOppslagClient: AktorOppslagClient,
    private val authService: AuthService,
    private val poaoTilgangClient: PoaoTilgangClient,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient,
    private val arenaService: ArenaOppfolgingService
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
        val erUnderOppfolging: Boolean = oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map<Boolean> { it.isUnderOppfolging }.orElse(false)
        return OppfolgingDto(erUnderOppfolging, null, fnr)
    }

    @QueryMapping
    fun veilederLeseTilgangModia(@Argument fnr: String?): VeilederTilgangDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return evaluerNavAnsattTilgangTilEksternBruker(fnr)
            .let {
                VeilederTilgangDto(
                    harTilgang = it == TilgangResultat.HAR_TILGANG,
                    tilgang = it
                )
            }
    }

    private fun evaluerNavAnsattTilgangTilEksternBruker(fnr: String): TilgangResultat {
        val decision = authService.evaluerNavAnsattTilagngTilBruker(Fnr.of(fnr), TilgangType.LESE)
        return when (decision) {
            is Decision.Deny -> decision.tryToFindDenyReason()
            Decision.Permit -> TilgangResultat.HAR_TILGANG
        }
    }

    private fun kanStarteOppfolgingMtpFregStatus(fnr: Fnr): FregStatusSjekkResultat {
        return pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr).toKanStarteOppfolging()
    }

    @SchemaMapping(typeName = "OppfolgingsEnhetsInfo", field = "enhet")
    fun arenaOppfolgingsEnhet(oppfolgingsEnhet: OppfolgingsEnhetQueryDto): EnhetDto? {
        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(oppfolgingsEnhet.fnr))
        val arenaEnhet = enhetRepository.hentEnhet(aktorId)
        return when {
            arenaEnhet == null -> hentDefaultEnhetFraNorg(Fnr.of(oppfolgingsEnhet.fnr))
            else -> arenaEnhet to KildeDto.ARENA
        }?.let { (enhetsNr, kilde) ->
            val enhet = runCatching {
                norg2Client.hentEnhet(enhetsNr.get())
            }.onFailure {
                logger.error("Kunne ikke slå opp enhet i norg", it)
            }.getOrNull()

            EnhetDto(
                id = enhetsNr.get(),
                navn = enhet?.navn?: "Ukjent enhet",
                kilde = kilde
            )
        }
    }

    @SchemaMapping(typeName = "OppfolgingDto", field = "kanStarteOppfolging")
    fun kanStarteOppfolging(oppfolgingDto: OppfolgingDto): KanStarteOppfolgingDto? {
        if (oppfolgingDto.norskIdent == null) throw InternFeil("Fant ikke fnr å sjekke tilgang mot i kanStarteOppfolging")
        val gyldigOppfolging = lazy {
            if (oppfolgingDto.erUnderOppfolging) {
                val erIservIArena = arenaService.brukerErIservIArena(Fnr.of(oppfolgingDto.norskIdent))
                if (erIservIArena) {
                    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
                } else ALLEREDE_UNDER_OPPFOLGING
            } else {
                OPPFOLGING_OK
            }
        }
        val gyldigTilgang = lazy { evaluerNavAnsattTilgangTilEksternBruker(oppfolgingDto.norskIdent).toKanStarteOppfolging() }
        val gyldigFregStatus = lazy { kanStarteOppfolgingMtpFregStatus(Fnr.of(oppfolgingDto.norskIdent)) }
        return oppfolgingSjekk(gyldigOppfolging, gyldigTilgang, gyldigFregStatus)
    }

    fun hentDefaultEnhetFraNorg(fnr: Fnr): Pair<EnhetId, KildeDto>? {
        val tilgangsattributterResponse = poaoTilgangClient.hentTilgangsAttributter(fnr.get())
        if (tilgangsattributterResponse.isFailure) throw PoaoTilgangError(tilgangsattributterResponse.exception!!)
        val tilgangsAttributter = tilgangsattributterResponse.getOrThrow()
        return tilgangsAttributter.kontor?.let { EnhetId.of(it) to KildeDto.NORG }
    }

    @QueryMapping
    fun gjeldendeOppfolgingsperiode(): OppfolgingsperiodeDto? {
        if (!authService.erEksternBruker()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        val innloggetBrukerFnr = authService.innloggetBrukerIdent
        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(innloggetBrukerFnr))
        return oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
            .map { it.toOppfolgingsperiodeDto() }
            .getOrNull()
    }

    private fun sjekkLeseTilgang(fnr: String?): Fnr {
        val userRole = authService.role.get()
        return when (userRole) {
            UserRole.EKSTERN -> {
                Fnr.of(authService.innloggetBrukerIdent)
            }
            UserRole.INTERN -> {
                fnr?.let { Fnr.of(it) }
                    ?.also { authService.sjekkLesetilgangMedFnr(it) }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd for systembruker")
            }
            UserRole.SYSTEM -> {
                fnr?.let { Fnr.of(it) }
                    ?.also { authService.sjekkLesetilgangMedFnr(it) }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd for systembruker")
            }
        }
    }

    @QueryMapping
    fun oppfolgingsPerioder(@Argument fnr: String?): List<OppfolgingsperiodeDto> {
        val fnr = sjekkLeseTilgang(fnr)
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            .map { it.toOppfolgingsperiodeDto() }
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

object AdGruppeNavn {
    const val STRENGT_FORTROLIG_ADRESSE = "0000-GA-Strengt_Fortrolig_Adresse"
    const val FORTROLIG_ADRESSE = "0000-GA-Fortrolig_Adresse"
    const val EGNE_ANSATTE = "0000-GA-Egne_ansatte"

    /* AKA modia generell tilgang, en av disse trengs for lese-tilgang */
    const val MODIA_OPPFOLGING = "0000-GA-Modia-Oppfolging"
    const val MODIA_GENERELL = "0000-GA-BD06_ModiaGenerellTilgang"
}

fun OppfolgingsperiodeEntity.toOppfolgingsperiodeDto(): OppfolgingsperiodeDto {
    return OppfolgingsperiodeDto(
        startTidspunkt = startDato.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        sluttTidspunkt = sluttDato?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        id = uuid.toString(),
        startetBegrunnelse.let {
            if (it == OppfolgingStartBegrunnelse.REAKTIVERT_OPPFØLGING) {
                it.name.replace("ø", "o")
            } else {
                it.name
            }
        }
    )
}