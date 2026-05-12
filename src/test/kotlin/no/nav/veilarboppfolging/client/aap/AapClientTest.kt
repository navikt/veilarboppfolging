package no.nav.veilarboppfolging.client.aap

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@WireMockTest
class AapClientTest {

    @Test
    fun `harAap - har aktiv periode i fremtiden - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val tilOgMed = LocalDate.now().plusMonths(6)
        @Language("JSON")
        val response = """
            {
              "perioder": [
                { "fraOgMedDato": "2025-01-01", "tilOgMedDato": "$tilOgMed" }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = AapClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(true, client.harAap("12345678910"))
    }

    @Test
    fun `harAap - periode med tilOgMedDato null - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "perioder": [
                { "fraOgMedDato": "2025-01-01", "tilOgMedDato": null }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = AapClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(true, client.harAap("12345678910"))
    }

    @Test
    fun `harAap - kun avsluttede perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "perioder": [
                { "fraOgMedDato": "2024-01-01", "tilOgMedDato": "2024-12-31" }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = AapClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, client.harAap("12345678910"))
    }

    @Test
    fun `harAap - ingen perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"perioder": []}""")
                )
        )
        val client = AapClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, client.harAap("12345678910"))
    }

    @Test
    fun `harAap - feilrespons fra tjenesten - kaster exception`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )
        val client = AapClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertThrows<RuntimeException> {
            client.harAap("12345678910")
        }
    }
}

