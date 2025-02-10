package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.pdl.GTType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester
import org.springframework.graphql.test.tester.GraphQlTester

class GraphqlControllerTest: IntegrationTest() {

    @Autowired
    private lateinit var graphQlSource: GraphQlSource

    @Test
    fun `skal oppfolgingsEnhet query`() {
        val fnr = Fnr("12345678910")
        mockGeografiskTilknytning(fnr, GTType.UDEFINERT, "1234")

        val service = DefaultExecutionGraphQlService(graphQlSource)
        val tester: GraphQlTester = ExecutionGraphQlServiceTester.create(service)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", fnr.get()).execute()
//        result.path("oppfolgingsEnhet.enhet").entity(EnhetDto::class.java).isEqualTo(null)
        result.path("oppfolgingsEnhet").entity(OppfolgingsEnhetQueryDto::class.java).isNotEqualTo(null)
    }
}
