package no.nav.veilarboppfolging.client.oppgave

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import java.time.LocalDate
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@WireMockTest
class OppgaveClientTest {
    @Test
    fun `opprettOppgave - ingen åpne oppgaver - oppretter oppgave og returnerer frist`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val finnOppgaveResponse = """
            {
              "antallTreffTotalt": 0,
              "oppgaver": []
            }
        """.trimIndent()
        givenThat(
            WireMock.get("/api/v1/oppgaver?tema=OPP&oppgavetype=KONT_BRUK&statuskategori=AAPEN&aktoerId=987654321")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(finnOppgaveResponse)
                )
        )
        val opprettOppgaveResponse = """
            {
              "id": 123,
              "fristFerdigstillelse": "2026-04-30"
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/api/v1/oppgaver")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(201)
                        .withBody(opprettOppgaveResponse)
                )
        )
        val oppgaveClient = OppgaveClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(LocalDate.of(2026, 4, 30), oppgaveClient.opprettOppgave("12345678910", "987654321"))
    }

    @Test
    fun `opprettOppgave - har åpen oppgave - returnerer frist fra eksisterende oppgave`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val finnOppgaveResponse = """
            {
              "antallTreffTotalt": 1,
              "oppgaver": [
                {
                  "id": 123,
                  "fristFerdigstillelse": "2026-04-30"
                }
              ]
            }
        """.trimIndent()
        givenThat(
            WireMock.get("/api/v1/oppgaver?tema=OPP&oppgavetype=KONT_BRUK&statuskategori=AAPEN&aktoerId=987654321")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(finnOppgaveResponse)
                )
        )
        val oppgaveClient = OppgaveClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(LocalDate.of(2026, 4, 30), oppgaveClient.opprettOppgave("12345678910", "987654321"))
    }
}