package no.nav.veilarboppfolging.client.pdl

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient
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
    fun pdlClient(
        tokenClient: AzureAdOnBehalfOfTokenClient,
        tokenXOnBehalfOfTokenClient: TokenXOnBehalfOfTokenClient,
        errorMappedAzureAdMachineToMachineTokenClient: ErrorMappedAzureAdMachineToMachineTokenClient,
    ): PdlClient {
        return PdlClientImpl(
            pdlUrl,
            {
                if (authService.erEksternBruker()) {
                    tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(
                        pdlScope
                            .replace("api://", "")
                            .replace("/.default", "")
                            .replace(".", ":")
                            .replace(".", ":"),
                        authService.innloggetBrukerToken)
                } else if (authService.erInternBruker()) {
                    tokenClient.exchangeOnBehalfOfToken(pdlScope, authService.innloggetBrukerToken)
                } else {
                    errorMappedAzureAdMachineToMachineTokenClient.createMachineToMachineToken(pdlScope)
                }
            },
            behandlingsnummer
        )
    }
}
