package no.nav.veilarboppfolging.client.tiltakshistorikk

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@WireMockTest
class TiltakshistorikkClientTest {
    @Test
    fun `harAktiveTiltaksdeltakelser - ingen tiltaksdeltakelser - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "historikk": [],
              "meldinger": []
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v1/historikk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )
        val tiltakshistorikkClient = TiltakshistorikkClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, tiltakshistorikkClient.harAktiveTiltaksdeltakelser("12345678910"))
    }

    @Test
    fun `harAktiveTiltaksdeltakelser - en avsluttet tiltaksdeltakelse - returnerer false`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "historikk": [
              {
                  "type": "TeamKometDeltakelse",
                  "norskIdent": "12345678910",
                  "startDato": "2024-04-04",
                  "sluttDato": "2024-04-05",
                  "id": "6d54228f-534f-4b4b-9160-65eae26a3b06",
                  "tittel": "Arbeidsforberedende trening hos Arrangør",
                  "status": {
                    "type": "HAR_SLUTTET",
                    "aarsak": "SYK",
                    "opprettetDato": "2024-04-04T14:32:32.003702"
                  },
                  "tiltakstype": {
                    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
                    "navn": "Arbeidsforberedende trening"
                  },
                  "gjennomforing": {
                    "id": "9caf398e-8e38-41fc-af29-b7ee6f62205a",
                    "navn": "Testgjennomføring",
                    "deltidsprosent": 100
                  },
                  "arrangor": {
                    "hovedenhet": null,
                    "underenhet": {
                    "organisasjonsnummer": "876543210",
                    "navn": "Arrangør"
                    }
                  },
                  "deltidsprosent": 60.0,
                  "dagerPerUke": 3.0,
                  "opphav": "TEAM_KOMET"
                }
              ],
              "meldinger": []
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v1/historikk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )
        val tiltakshistorikkClient = TiltakshistorikkClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(false, tiltakshistorikkClient.harAktiveTiltaksdeltakelser("12345678910"))
    }

    @Test
    fun `harAktiveTiltaksdeltakelser - en aktiv tiltaksdeltakelse - returnerer true`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "historikk": [
              {
                  "type": "TeamTiltakAvtale",
                  "norskIdent": "12345678910",
                  "startDato": "2024-01-01",
                  "sluttDato": "2024-12-31",
                  "id": "9dea48c1-d494-4664-9427-bdb20a6f265f",
                  "tittel": "Arbeidstrening hos Arbeidsgiver",
                  "tiltakstype": {
                    "tiltakskode": "ARBEIDSTRENING",
                    "navn": "Arbeidstrening"
                  },
                  "status": "GJENNOMFORES",
                  "stillingsprosent": 100,
                  "dagerPerUke": 5,
                  "arbeidsgiver": {
                    "organisasjonsnummer": "876543210",
                    "navn": "Arbeidsgiver"
                  },
                  "opphav": "TEAM_TILTAK"
                }
              ],
              "meldinger": []
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v1/historikk")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )
        val tiltakshistorikkClient = TiltakshistorikkClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(true, tiltakshistorikkClient.harAktiveTiltaksdeltakelser("12345678910"))
    }
}