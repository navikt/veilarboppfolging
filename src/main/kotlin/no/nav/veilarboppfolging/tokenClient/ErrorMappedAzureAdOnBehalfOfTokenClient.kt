package no.nav.veilarboppfolging.tokenClient

import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.veilarboppfolging.NetworkException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ErrorMappedAzureAdOnBehalfOfTokenClient {
    val aadOboTokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient()

    fun exchangeOnBehalfOfToken(scope: String, acccessToken: String): String {
        try {
            return aadOboTokenClient.exchangeOnBehalfOfToken(scope, acccessToken)
        } catch (socketTimeout: SocketTimeoutException) {
            throw NetworkException("Feilet å lage obo token: SocketTimeoutException")
        } catch (sslHandShakeException: SSLHandshakeException) {
            throw NetworkException("Feilet å lage obo token: SSLHandshakeException")
        }
    }
}