package no.nav.veilarboppfolging.client.pdl

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PdlClientConfig(
    @Value("\${app.env.pdlUrl}") val pdlUrl: String,
    @Value("\${app.env.pdlScope}") val pdlScope: String,
    @Value("\${app.env.pdlBehandlingsNummer}") val behandlingsnummer: String,
    private val authService: AuthService
) {

    @Bean
    fun pdlClient(tokenClient: AzureAdOnBehalfOfTokenClient): PdlClient {
        return PdlClientImpl(
            pdlUrl,
            { tokenClient.exchangeOnBehalfOfToken(pdlScope, authService.innloggetBrukerToken) },
            behandlingsnummer
        )
    }

}