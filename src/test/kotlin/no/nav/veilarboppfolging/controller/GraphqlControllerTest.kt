package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class GraphqlControllerTest: IntegrationTest() {

    @Autowired
    private lateinit var graphQlSource: GraphQlSource

    @Test
    fun `skal returnere oppfolgingsEnhet`() {
        val fnr = Fnr("12345678910")
        val aktorId = AktorId("22345678910")
        val kontor = "7414"
        val kontorNavn = "Nav Graphql Kontor"
        val skjermet = false
        mockAuthOk(aktorId, fnr)
        mockPdlGeografiskTilknytning(fnr, kontor)
        mockPoaoTilgangTilgangsAttributter(kontor, skjermet)
        mockNorgFinnNavKontor(kontor, kontorNavn, skjermet, false)
        val service = DefaultExecutionGraphQlService(graphQlSource)
        val tester = ExecutionGraphQlServiceTester.create(service)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolgingsEnhet.enhet").matchesJson("""
            { "id": "${kontor}", "kilde": "NORG", "navn": "${kontorNavn}" }
        """.trimIndent())
    }
}
