package no.nav.veilarboppfolging.controller

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingsEnhetService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException

data class OppfolgingsEnhetQueryDto(
    val enhet: EnhetDto?, // Nullable because graphql
    val kilde: KildeDto,
    val fnr: String // Only used to pass fnr to "sub-queries"
)

data class EnhetDto(
    val id: String,
    val navn: String
)

enum class KildeDto {
    ARENA
}

data class OppfolgingDto(
    val erUnderOppfolging: Boolean,
)

@Controller
class GraphqlController(
    private val oppfolgingsEnhetService: OppfolgingsEnhetService,
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository,
    private val norg2Client: Norg2Client,
    private val aktorOppslagClient: AktorOppslagClient,
    private val authService: AuthService
) {

    @QueryMapping
    fun oppfolgingsEnhet(@Argument fnr: String?): OppfolgingsEnhetQueryDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return OppfolgingsEnhetQueryDto(fnr = fnr, enhet = null, kilde = KildeDto.ARENA)
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
        return oppfolgingsEnhetService.getOppfolgingsEnhet(aktorId)
            ?.let { oppfolgingsenhet ->
                val enhet = norg2Client.hentEnhet(oppfolgingsenhet.enhet)
                EnhetDto(
                    id = oppfolgingsenhet.enhet,
                    navn = enhet.navn
                )
            }
    }
}