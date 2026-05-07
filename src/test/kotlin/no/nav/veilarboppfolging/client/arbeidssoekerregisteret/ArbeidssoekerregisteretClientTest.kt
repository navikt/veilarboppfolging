package no.nav.veilarboppfolging.client.arbeidssoekerregisteret

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
class ArbeidssoekerregisteretClientTest {

    @Test
    fun `erArbeidssoeker - har aktiv arbeidssoekerperiode - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            [
              {
                "periodeId": "11111111-1111-1111-1111-111111111111",
                "startet": { "tidspunkt": "2026-01-01T00:00:00Z" },
                "avsluttet": null
              }
            ]
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v3/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = ArbeidssoekerregisteretClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(true, client.erArbeidssoeker("12345678910"))
    }

    @Test
    fun `erArbeidssoeker - kun avsluttede perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            [
              {
                "periodeId": "11111111-1111-1111-1111-111111111111",
                "startet": { "tidspunkt": "2025-01-01T00:00:00Z" },
                "avsluttet": { "tidspunkt": "2025-06-01T00:00:00Z" }
              }
            ]
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v3/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = ArbeidssoekerregisteretClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, client.erArbeidssoeker("12345678910"))
    }

    @Test
    fun `erArbeidssoeker - ingen perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/api/v3/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )
        val client = ArbeidssoekerregisteretClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, client.erArbeidssoeker("12345678910"))
    }

    @Test
    fun `erArbeidssoeker - feilrespons fra tjenesten - kaster exception`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/api/v3/perioder")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )
        val client = ArbeidssoekerregisteretClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertThrows<RuntimeException> {
            client.erArbeidssoeker("12345678910")
        }
    }
}

