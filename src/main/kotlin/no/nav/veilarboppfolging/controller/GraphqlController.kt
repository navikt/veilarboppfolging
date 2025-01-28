package no.nav.veilarboppfolging.controller

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.client.pdl.PdlClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.norg.Enhet
import no.nav.veilarboppfolging.client.norg.INorgTilhorighetClient
import no.nav.veilarboppfolging.client.norg.NorgTilhorighetRequest
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningClient
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
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

data class OppfolgingDto(
    val erUnderOppfolging: Boolean,
)

@Controller
class GraphqlController(
    private val enhetRepository: EnhetRepository,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val norg2Client: Norg2Client,
    private val aktorOppslagClient: AktorOppslagClient,
    private val authService: AuthService,
    private val geografiskTilknytningClient: GeografiskTilknytningClient,
    private val INorgTilhorighetClient: INorgTilhorighetClient
) {

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

        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(fnr))
        val maybeOppfolgingsStatus = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        return OppfolgingDto(erUnderOppfolging = maybeOppfolgingsStatus.map { it.isUnderOppfolging }.orElse(false))
    }

    @SchemaMapping(typeName="OppfolgingsEnhetsInfo", field="enhet")
    fun arenaOppfolgingsEnhet(oppfolgingsEnhet: OppfolgingsEnhetQueryDto): EnhetDto? {
        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(oppfolgingsEnhet.fnr))
        val arenaEnhet = enhetRepository.hentEnhet(aktorId)
            ?.let { oppfolgingsenhet ->
                val enhet = norg2Client.hentEnhet(oppfolgingsenhet.get())
                EnhetDto(
                    id = oppfolgingsenhet.get(),
                    navn = enhet.navn,
                    kilde = KildeDto.ARENA
                )
            }
        return when {
            arenaEnhet == null -> hentDefaultEnhetFraNorg(Fnr.of(oppfolgingsEnhet.fnr))
            else -> arenaEnhet
        }
    }

    fun hentDefaultEnhetFraNorg(fnr: Fnr): EnhetDto? {
        return geografiskTilknytningClient.hentGeografiskTilknytning(fnr)
            .let {
                when (it.geografiskTilknytning) {
                    null  -> null
                    else -> INorgTilhorighetClient.hentTilhorendeEnhet(
                        NorgTilhorighetRequest(
                            GeografiskTilknytningNr(it.geografiskTilknytning.gtType, it.geografiskTilknytning.nr),
                            false,
                            false
                        ))
                }
            }
            ?.let { enhet: Enhet ->
                EnhetDto(
                    id = enhet.enhetNr,
                    navn = enhet.enhetNavn,
                    kilde = KildeDto.NORG
                )
            }
    }
}