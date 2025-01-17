package no.nav.veilarboppfolging.controller

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingsEnhetService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException

data class OppfolgingsEnhetDto(
    val arenaOppfolgingsEnhet: ArenaOppfolgingsEnhetDto? // Nullable because graphql
)

data class ArenaOppfolgingsEnhetDto(
    val id: String,
    val navn: String
)

@Controller
class GraphqlController(
    private val oppfolgingsEnhetService: OppfolgingsEnhetService,
    private val norg2Client: Norg2Client,
    private val aktorOppslagClient: AktorOppslagClient,
    private val authService: AuthService
) {

    @QueryMapping
    fun oppfolgingsEnhet(@Argument fnr: String?): OppfolgingsEnhetDto {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        if (authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return OppfolgingsEnhetDto(null)
    }

    @SchemaMapping(typeName="OppfolgingsEnheter", field="arenaOppfolgingsEnhet")
    fun arenaOppfolgingsEnhet(@Argument fnr: String?): ArenaOppfolgingsEnhetDto? {
        if (fnr == null || fnr.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr er påkrevd")
        val fnr = Fnr.of(fnr)

        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        return oppfolgingsEnhetService.getOppfolgingsEnhet(aktorId)
            ?.let { oppfolgingsenhet ->
                val enhet = norg2Client.hentEnhet(oppfolgingsenhet.enhet)
                ArenaOppfolgingsEnhetDto(
                    id = oppfolgingsenhet.enhet,
                    navn = enhet.navn
                )
            }
    }
}