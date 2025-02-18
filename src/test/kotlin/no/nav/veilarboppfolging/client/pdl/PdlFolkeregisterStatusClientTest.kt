package no.nav.veilarboppfolging.client.pdl

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.types.identer.Fnr
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@WireMockTest
class PdlFolkeregisterStatusClientTest {

    @Test
    fun `skal deserialisere hentPerson (folkeregisterpersonstatus) riktig`(wmRuntimeInfo: WireMockRuntimeInfo ) {
        val apiUrl = "http://localhost:" + wmRuntimeInfo.httpPort
        @Language("JSON")
        val response = """
            {
              "data": {
                "hentPerson": {
                  "folkeregisterpersonstatus": [
                    {
                      "forenkletStatus": "doedIFolkeregisteret"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        givenThat(
            WireMock.post("/graphql")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )

        val pdlClient = PdlClientImpl(apiUrl, { "token" }, "B1234")
        val result = PdlFolkeregisterStatusClient(pdlClient).hentFolkeregisterStatus(Fnr("12345678910"))
        assertEquals(result, ForenkletFolkeregisterStatus.doedIFolkeregisteret)
    }

}