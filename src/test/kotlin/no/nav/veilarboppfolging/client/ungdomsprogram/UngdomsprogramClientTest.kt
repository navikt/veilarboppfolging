package no.nav.veilarboppfolging.client.ungdomsprogram

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@WireMockTest
class UngdomsprogramClientTest {

    @Test
    fun `erDeltakerIUngdomsprogrammet - har aktiv deltakelse - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "harAktivDeltakelse": true
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/ekstern/deltakelse/sjekk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = UngdomsprogramClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(true, client.erDeltakerIUngdomsprogrammet("12345678910"))
    }

    @Test
    fun `erDeltakerIUngdomsprogrammet - har ikke aktiv deltakelse - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "harAktivDeltakelse": false
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/ekstern/deltakelse/sjekk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = UngdomsprogramClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, client.erDeltakerIUngdomsprogrammet("12345678910"))
    }

    @Test
    fun `erDeltakerIUngdomsprogrammet - feilrespons fra tjenesten - kaster exception`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/ekstern/deltakelse/sjekk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )
        val client = UngdomsprogramClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertThrows<RuntimeException> {
            client.erDeltakerIUngdomsprogrammet("12345678910")
        }
    }
}

