package no.nav.veilarboppfolging.client.oppgave

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import java.time.LocalDate
import kotlin.test.assertEquals
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@WireMockTest
class OppgaveClientTest {
    private val fnr = Fnr.of("12345678910")
    private val aktorId = AktorId.of("987654321")

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
            WireMock.get("/api/v1/oppgaver?tema=OPP&oppgavetype=KONT_BRUK&statuskategori=AAPEN&aktoerId=${aktorId.get()}")
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

        assertEquals(LocalDate.of(2026, 4, 30), oppgaveClient.opprettOppgave(fnr, aktorId))
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
            WireMock.get("/api/v1/oppgaver?tema=OPP&oppgavetype=KONT_BRUK&statuskategori=AAPEN&aktoerId=${aktorId.get()}")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(finnOppgaveResponse)
                )
        )
        val oppgaveClient = OppgaveClient(apiUrl, { "token" }, OkHttpClient.Builder().build())

        assertEquals(LocalDate.of(2026, 4, 30), oppgaveClient.opprettOppgave(fnr, aktorId))
    }
}