package no.nav.veilarboppfolging.controller.graphql

import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.controller.FantIkkeAktorIdForFnrError
import no.nav.veilarboppfolging.controller.PoaoTilgangError
import no.nav.veilarboppfolging.controller.graphql.brukerStatus.BrukerStatusArenaDto
import no.nav.veilarboppfolging.controller.graphql.brukerStatus.BrukerStatusDto
import no.nav.veilarboppfolging.controller.graphql.brukerStatus.BrukerStatusKrrDto
import no.nav.veilarboppfolging.controller.graphql.brukerStatus.BrukerStatusManuellDto
import no.nav.veilarboppfolging.controller.graphql.brukerStatus.KontorSperre
import no.nav.veilarboppfolging.controller.graphql.oppfolging.EnhetDto
import no.nav.veilarboppfolging.controller.graphql.oppfolging.KildeDto
import no.nav.veilarboppfolging.controller.graphql.oppfolging.OppfolgingDto
import no.nav.veilarboppfolging.controller.graphql.oppfolging.OppfolgingsEnhetQueryDto
import no.nav.veilarboppfolging.controller.graphql.oppfolging.OppfolgingsperiodeDto
import no.nav.veilarboppfolging.controller.graphql.veilederTilgang.VeilederTilgangDto
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.*
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING.oppfolgingSjekk
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.KvpRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.ManuellStatusService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.LocalContextValue
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull
import kotlin.text.isEmpty

enum class TilgangResultat {
    HAR_TILGANG,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA
}

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
    private val arenaService: ArenaOppfolgingService,
    private val manuellService: ManuellStatusService,
    private val oppfolgingService: OppfolgingService,
    private val kvpRepository: KvpRepository
) {
    private val logger = LoggerFactory.getLogger(GraphqlController::class.java)

    init {
        logger.info("Started GraphqlController")
    }

    @QueryMapping
    fun oppfolgingsEnhet(@Argument fnr: String?): OppfolgingsEnhetQueryDto {
        val fnr = sjekkTilgang(fnr, Tilgang.IKKE_EKSTERNBRUKERE).getFnrFromContextOrThrow()

        return OppfolgingsEnhetQueryDto(fnr = fnr.get(), enhet = null)
    }

    private fun erUnderOppfolging(aktorId: AktorId): Boolean {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map { it.isUnderOppfolging }.orElse(false)
    }

    @QueryMapping
    fun oppfolging(@Argument fnr: String?): DataFetcherResult<OppfolgingDto> {
        val tilgangResult = sjekkTilgang(fnr, Tilgang.IKKE_EKSTERNBRUKERE)
        val fnr = fnrFraContext(fnr)

        val dataFetchResult = DataFetcherResult.newResult<OppfolgingDto>()
        val aktorId = aktorOppslagClient.hentAktorId(fnr) as AktorId? ?: throw FantIkkeAktorIdForFnrError()
        val erUnderOppfolging = erUnderOppfolging(aktorId)
        val localContext = GraphQLContext.getDefault()
            .put("fnr", fnr)
            .put("aktorId", aktorId)
            .put("erUnderOppfolging", erUnderOppfolging)

        val data = when (tilgangResult) {
            is HarIkkeTilgang -> OppfolgingDto(null, null)
            is HarTilgang -> OppfolgingDto(erUnderOppfolging, null)
        }

        return dataFetchResult.localContext(localContext).data(data).build()
    }

    @QueryMapping
    fun veilederLeseTilgangModia(@Argument fnr: String?): DataFetcherResult<VeilederTilgangDto> {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        val result = DataFetcherResult.newResult<VeilederTilgangDto>()
        val context = GraphQLContext.getDefault().put("fnr", Fnr.of(fnr))
        return evaluerNavAnsattTilgangTilEksternBruker(fnr)
            .let {
                VeilederTilgangDto(
                    harTilgang = it == TilgangResultat.HAR_TILGANG,
                    harVeilederLeseTilgangTilBruker = it == TilgangResultat.HAR_TILGANG,
                    tilgang = it,
                    harVeilederLeseTilgangTilKontorsperretBruker = null,
                    harVeilederLeseTilgangTilBrukersEnhet = null
                )
            }.let { result.localContext(context).data(it).build() }
    }

    @SchemaMapping(typeName = "VeilederTilgang", field = "harVeilederLeseTilgangTilBrukersKontorsperre")
    fun harVeilederLeseTilgangTilBrukersKontorsperre(tilgang: VeilederTilgangDto, @LocalContextValue fnr: Fnr): Boolean {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        return oppfolgingService.harVeilederTilgangTilKontorsperretEnhet(aktorId)
    }

    @QueryMapping
    fun brukerStatus(@Argument fnr: String?): DataFetcherResult<BrukerStatusDto> {
        val fnr = sjekkTilgang(fnr, Tilgang.ALLE).getFnrFromContextOrThrow()
        val result = DataFetcherResult.newResult<BrukerStatusDto>()
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        val localContext = GraphQLContext.getDefault()
            .put("fnr", fnr)
            .put("aktorId", aktorId)
        return result.localContext(localContext)
            .data(BrukerStatusDto())
            .build()
    }


    fun fnrFraContext(fnr: String?): Fnr {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) return Fnr.of(authService.innloggetBrukerIdent) else return Fnr.of(fnr)
    }

    fun TilgangsSjekkResultat.getFnrFromContextOrThrow() = when (this) {
        is HarIkkeTilgang -> throw ResponseStatusException(HttpStatus.FORBIDDEN)
        is HarTilgang -> this.fnr
    }
    private fun sjekkTilgang(inputFnr: String?, tilgang: Tilgang): TilgangsSjekkResultat {
        val fnr = fnrFraContext(inputFnr)
        val role = authService.role.getOrNull() ?: throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return when (role) {
             UserRole.EKSTERN -> {
                when (tilgang) {
                    Tilgang.ALLE -> {
                        return if (!authService.harEksternBrukerTilgang(fnr)) HarIkkeTilgang("Eksternbrukere har bare tilgang til seg selv")
                        else HarTilgang(Fnr.of(authService.innloggetBrukerIdent))
                    }
                    Tilgang.IKKE_EKSTERNBRUKERE -> HarIkkeTilgang("Eksternbrukere har ikke tilgang til dette API-et")
                }
            }
            UserRole.INTERN -> {
                val isAllowed = authService.evaluerNavAnsattTilagngTilBruker(fnr, TilgangType.LESE)
                return when (isAllowed) {
                    is Decision.Deny -> HarIkkeTilgang("Veileder har ikke tilgang til bruker")
                    Decision.Permit -> HarTilgang(fnr)
                }
            }
            UserRole.SYSTEM -> HarTilgang(fnr)
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
                navn = enhet?.navn ?: "Ukjent enhet",
                kilde = kilde
            )
        }
    }

    @SchemaMapping(typeName = "OppfolgingDto", field = "kanStarteOppfolging")
    fun kanStarteOppfolging(oppfolgingDto: OppfolgingDto, @LocalContextValue erUnderOppfolging: Boolean, @LocalContextValue fnr: Fnr): KanStarteOppfolgingDto? {
        val gyldigOppfolging = lazy {
            if (erUnderOppfolging) {
                val erIservIArena = arenaService.brukerErIservIArena(fnr)
                if (erIservIArena) {
                    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
                } else ALLEREDE_UNDER_OPPFOLGING
            } else {
                OPPFOLGING_OK
            }
        }
        val gyldigTilgang = lazy { evaluerNavAnsattTilgangTilEksternBruker(fnr.get()).toKanStarteOppfolging() }
        val gyldigFregStatus = lazy { kanStarteOppfolgingMtpFregStatus(fnr) }
        return oppfolgingSjekk(gyldigOppfolging, gyldigTilgang, gyldigFregStatus)
    }

    @SchemaMapping(typeName = "BrukerStatusDto", field = "manuell")
    fun manuell(brukerStatusDto: BrukerStatusDto, @LocalContextValue aktorId: AktorId): BrukerStatusManuellDto? {
        return manuellService.hentManuellStatus(aktorId)
            .map { BrukerStatusManuellDto(
                it.isManuell,
                it.dato.toString(),
                it.begrunnelse,
                it.opprettetAv.toString(),
                it.opprettetAvBrukerId
            ) }
            .getOrNull()
    }

    @SchemaMapping(typeName = "BrukerStatusDto", field = "erKontorsperret")
    fun erKontorsperret(brukerStatusDto: BrukerStatusDto, @LocalContextValue aktorId: AktorId): Boolean? {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .map { it.gjeldendeKvpId != null && it.gjeldendeKvpId != 0L }.orElse(false)
    }

    @SchemaMapping(typeName = "BrukerStatusDto", field = "kontorSperre")
    fun kontorSperre(brukerStatusDto: BrukerStatusDto, @LocalContextValue aktorId: AktorId): KontorSperre? {
        return kvpRepository.hentGjeldendeKvpPeriode(aktorId)
            .map { it.enhet }.getOrNull()
            ?.let { KontorSperre(it) }
    }

    @SchemaMapping(typeName = "BrukerStatusDto", field = "krr")
    fun reservertIKrr(brukerStatusDto: BrukerStatusDto, @LocalContextValue fnr: Fnr): BrukerStatusKrrDto? {
        val result = manuellService.hentDigdirKontaktinfo(fnr)
        return BrukerStatusKrrDto(
            kanVarsles = result.isKanVarsles,
            registrertIKrr = result.isAktiv,
            reservertIKrr = result.isReservert
        )
    }

    @SchemaMapping(typeName = "BrukerStatusDto", field = "arena")
    fun arena(brukerStatusDto: BrukerStatusDto, @LocalContextValue aktorId: AktorId): BrukerStatusArenaDto? {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
            .flatMap { it.localArenaOppfolging }
            .map {
                BrukerStatusArenaDto(
                    inaktivIArena = it.formidlingsgruppe == Formidlingsgruppe.ISERV,
                    kanReaktiveres = null,
                    inaktiveringsdato = it.iservFraDato?.toString(),
                    kvalifiseringsgruppe = it.kvalifiseringsgruppe.toString()
                )
            }.getOrNull()
    }

    @SchemaMapping(typeName = "BrukerStatusArenaDto", field = "kanReaktiveres")
    fun kanReaktiveres(brukerStatusArenaDto: BrukerStatusArenaDto, @LocalContextValue fnr: Fnr): Boolean? {
        return arenaService.hentArenaOppfolgingsStatus(fnr)
            .map { it.kanEnkeltReaktiveres }.orElse(null)
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
            UserRole.SYSTEM,
            UserRole.INTERN -> {
                fnr?.let { Fnr.of(it) }
                    ?.also { authService.sjekkLesetilgangMedFnr(it) }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd for ${userRole.name}-bruker")
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
        startetBegrunnelse?.let {
            if (it == OppfolgingStartBegrunnelse.REAKTIVERT_OPPFØLGING) {
                it.name.replace("ø", "o")
            } else {
                it.name
            }
        }
    )
}

enum class Tilgang {
    ALLE,
    IKKE_EKSTERNBRUKERE
}

sealed class TilgangsSjekkResultat
class HarTilgang(val fnr: Fnr): TilgangsSjekkResultat()
class HarIkkeTilgang(val message: String): TilgangsSjekkResultat()
