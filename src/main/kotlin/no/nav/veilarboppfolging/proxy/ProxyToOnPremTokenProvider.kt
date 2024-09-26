package no.nav.veilarboppfolging.proxy

import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.service.AuthService
import org.springframework.stereotype.Component

@Component
class ProxyToOnPremTokenProvider(
    val authService: AuthService,
    val azureAdOnBehalfOfTokenClient: AzureAdOnBehalfOfTokenClient,
    val machineToMachineTokenClient: MachineToMachineTokenClient,
    val tokenXOnBehalfOfTokenClient: TokenXOnBehalfOfTokenClient
) {

    private val isProd: Boolean = EnvironmentUtils.isProduction().orElse(false)
    private val scope = String.format("api://%s-fss.pto.veilarboppfolging/.default", if (isProd) "prod" else "dev")
    private val audience = String.format("%s-fss:pto:veilarboppfolging", if (isProd) "prod" else "dev")

    fun getProxyToken(): String {
        return if (authService.erInternBruker()) azureAdOnBehalfOfTokenClient!!.exchangeOnBehalfOfToken(
            scope,
            authService.innloggetBrukerToken
        )
        else if (authService.erEksternBruker()) tokenXOnBehalfOfTokenClient!!.exchangeOnBehalfOfToken(
            audience,
            authService.innloggetBrukerToken
        )
        else if (authService.erSystemBruker()) machineToMachineTokenClient!!.createMachineToMachineToken(scope)
        else throw RuntimeException("Klarte ikke å identifisere brukertype")
    }
}