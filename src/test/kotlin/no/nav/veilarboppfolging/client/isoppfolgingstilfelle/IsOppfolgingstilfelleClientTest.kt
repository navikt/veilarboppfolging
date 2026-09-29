package no.nav.veilarboppfolging.client.isoppfolgingstilfelle

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import no.nav.common.types.identer.Fnr
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@WireMockTest
class IsOppfolgingstilfelleClientTest {

    @Test
    fun `hentStatus - aktivt tilfelle med arbeidsgiver - returnerer riktig status`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "oppfolgingstilfelleList": [
                {
                  "arbeidstakerAtTilfelleEnd": true,
                  "start": "2026-09-01",
                  "end": "2026-10-01",
                  "antallSykedager": 10,
                  "varighetUker": 4,
                  "virksomhetsnummerList": ["123456789"]
                }
              ],
              "personIdent": "12345678910",
              "dodsdato": null,
              "hasGjentakendeSykefravar": false
            }
        """.trimIndent()
        givenThat(
            WireMock.get("/api/system/v1/oppfolgingstilfelle/personident")
                .withHeader("nav-personident", WireMock.equalTo("12345678910"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = IsOppfolgingstilfelleClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(OppfolgingstilfelleStatus.SYKMELDT_MED_ARBEIDSGIVER, client.hentStatus("12345678910"))
    }

    @Test
    fun `hentStatus - aktivt tilfelle uten arbeidsgiver - returnerer riktig status`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "oppfolgingstilfelleList": [
                {
                  "arbeidstakerAtTilfelleEnd": false,
                  "start": "2026-09-01",
                  "end": "2026-10-01",
                  "antallSykedager": 10,
                  "varighetUker": 4,
                  "virksomhetsnummerList": ["123456789"]
                }
              ],
              "personIdent": "12345678910",
              "dodsdato": null,
              "hasGjentakendeSykefravar": false
            }
        """.trimIndent()
        givenThat(
            WireMock.get("/api/system/v1/oppfolgingstilfelle/personident")
                .withHeader("nav-personident", WireMock.equalTo("12345678910"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = IsOppfolgingstilfelleClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(OppfolgingstilfelleStatus.SYKMELDT_UTEN_ARBEIDSGIVER, client.hentStatus("12345678910"))
    }

    @Test
    fun `hentStatus - ingen aktivt tilfelle - returnerer null`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "oppfolgingstilfelleList": [
                {
                  "arbeidstakerAtTilfelleEnd": true,
                  "start": "2026-01-01",
                  "end": "2026-01-31",
                  "antallSykedager": 10,
                  "varighetUker": 4,
                  "virksomhetsnummerList": ["123456789"]
                }
              ],
              "personIdent": "12345678910",
              "dodsdato": null,
              "hasGjentakendeSykefravar": false
            }
        """.trimIndent()
        givenThat(
            WireMock.get("/api/system/v1/oppfolgingstilfelle/personident")
                .withHeader("nav-personident", WireMock.equalTo("12345678910"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
        val client = IsOppfolgingstilfelleClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertNull(client.hentStatus("12345678910"))
    }

    @Test
    fun `hentStatus - feilrespons fra tjenesten - kaster exception`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.get("/api/system/v1/oppfolgingstilfelle/personident")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                )
        )
        val client = IsOppfolgingstilfelleClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertThrows<RuntimeException> {
            client.hentStatus(Fnr.of("12345678910").get())
        }
    }
}
