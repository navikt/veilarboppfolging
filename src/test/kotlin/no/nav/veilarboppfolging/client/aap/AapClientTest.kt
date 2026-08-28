package no.nav.veilarboppfolging.client.aap

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import java.time.LocalDate
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@WireMockTest
class AapClientTest {

    @Test
    fun `harAap - har aktiv Arena-periode nå - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().minusWeeks(1)
        val tilOgMed = LocalDate.now().plusMonths(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "IVERK",
                  "periode": {
                    "fraOgMedDato": "$fraOgMed",
                    "tilOgMedDato": "$tilOgMed"
                  },
                  "kilde": "ARENA"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - har aktiv Arena-periode i fremtiden - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().plusDays(1)
        val tilOgMed = LocalDate.now().plusMonths(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "IVERK",
                  "periode": {
                    "fraOgMedDato": "$fraOgMed",
                    "tilOgMedDato": "$tilOgMed"
                  },
                  "kilde": "ARENA"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - har aktiv Arena-periode med tilOgMedDato null - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().plusDays(1)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "IVERK",
                  "periode": {
                    "fraOgMedDato": "$fraOgMed",
                    "tilOgMedDato": null
                  },
                  "kilde": "ARENA"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - kun avsluttede Arena-perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().minusYears(1)
        val tilOgMed = LocalDate.now().minusWeeks(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "IVERK",
                  "periode": {
                    "fraOgMedDato": "$fraOgMed",
                    "tilOgMedDato": "$tilOgMed"
                  },
                  "kilde": "ARENA"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - tom Arena-periode men status OPPRE - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "OPPRE",
                  "periode": {
                    "fraOgMedDato": null,
                    "tilOgMedDato": null
                  },
                  "kilde": "ARENA"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - har aktiv Kelvin-periode i fremtiden - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().plusDays(1)
        val tilOgMed = LocalDate.now().plusMonths(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statuskode": "FERDIGBEHANDLET",
                  "perioder": [
                    {
                      "fraOgMedDato": "$fraOgMed",
                      "tilOgMedDato": "$tilOgMed"
                    }
                  ],
                  "ytelsesstatus": "LOPENDE",
                  "kilde": "KELVIN"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - har aktiv Kelvin-periode nå - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().minusDays(1)
        val tilOgMed = LocalDate.now().plusMonths(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statuskode": "FERDIGBEHANDLET",
                  "perioder": [
                    {
                      "fraOgMedDato": "$fraOgMed",
                      "tilOgMedDato": "$tilOgMed"
                    }
                  ],
                  "ytelsesstatus": "LOPENDE",
                  "kilde": "KELVIN"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - kun avsluttede Kelvin-perioder - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().minusYears(1)
        val tilOgMed = LocalDate.now().minusWeeks(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statuskode": "FERDIGBEHANDLET",
                  "perioder": [
                    {
                      "fraOgMedDato": "$fraOgMed",
                      "tilOgMedDato": "$tilOgMed"
                    }
                  ],
                  "ytelsesstatus": "LOPENDE",
                  "kilde": "KELVIN"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - har avsluttet Arena-periode og mottatt søknad i Kelvin - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        val fraOgMed = LocalDate.now().minusYears(1)
        val tilOgMed = LocalDate.now().minusWeeks(6)
        @Language("JSON")
        val response = """
            {
              "saker": [
                {
                  "sakId": "123",
                  "statusKode": "IVERK",
                  "periode": {
                    "fraOgMedDato": "$fraOgMed",
                    "tilOgMedDato": "$tilOgMed"
                  },
                  "kilde": "ARENA"
                },
                {
                  "sakId": "123",
                  "statuskode": "SOKNAD_UNDER_BEHANDLING",
                  "perioder": [],
                  "ytelsesstatus": "FOR_VEDTAK",
                  "kilde": "KELVIN"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - ingen saker - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "saker": []
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
    fun `harAap - feilrespons fra tjenesten - kaster exception`(wmRuntimeInfo: WireMockRuntimeInfo) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        givenThat(
            WireMock.post("/dab/sakerByFnr")
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
