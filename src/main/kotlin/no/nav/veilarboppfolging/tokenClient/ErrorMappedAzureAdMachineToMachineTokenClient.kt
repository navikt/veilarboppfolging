package no.nav.veilarboppfolging.tokenClient

import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.veilarboppfolging.NetworkException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ErrorMappedAzureAdMachineToMachineTokenClient(
) {
    private val azureTokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildMachineToMachineTokenClient()

    fun createMachineToMachineToken(scope: String): String {
        try {
            return azureTokenClient.createMachineToMachineToken(scope)
        } catch (socketTimeout: SocketTimeoutException) {
            throw NetworkException("Feilet å lage machine token: SocketTimeoutException")
        } catch (sslHandShakeException: SSLHandshakeException) {
            throw NetworkException("Feilet å lage machine token: SSLHandshakeException")
        }
    }
}