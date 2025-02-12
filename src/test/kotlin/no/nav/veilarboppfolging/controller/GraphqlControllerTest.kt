package no.nav.veilarboppfolging.controller

import graphql.ErrorType
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

    private val service: DefaultExecutionGraphQlService by lazy {
        DefaultExecutionGraphQlService(graphQlSource)
    }
    private val tester: ExecutionGraphQlServiceTester by lazy {
        ExecutionGraphQlServiceTester.create(service)
    }

    fun defaultBruker(): Pair<Fnr, AktorId> {
        val fnr = Fnr("12345678910")
        val aktorId = AktorId("22345678910")
        mockAuthOk(aktorId, fnr)
        return fnr to aktorId
    }

    @Test
    fun `skal returnere oppfolgingsEnhet`() {
        val (fnr, _) = defaultBruker()
        val kontor = "7414"
        val kontorNavn = "Nav Graphql Kontor"
        val skjermet = false
        mockPdlGeografiskTilknytning(fnr, kontor)
        mockPoaoTilgangTilgangsAttributter(kontor, skjermet)
        mockNorgEnhetsNavn(kontor, kontorNavn)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolgingsEnhet.enhet").matchesJson("""
            { "id": "${kontor}", "kilde": "NORG", "navn": "${kontorNavn}" }
        """.trimIndent())
    }

    @Test
    fun `skal returnere error på oppfolgingsEnhet når noe skjer`() {
        val (fnr, _) = defaultBruker()
        mockPoaoTilgangTilgangsAttributterFeiler()
        val expectedError = PoaoTilgangError(IllegalArgumentException("LOL"))

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", fnr.get()).execute()
        result.errors()
            .expect { it.message.equals(expectedError.toString()) }
            .expect { it.errorType.equals(expectedError.errorType) }
            .verify()
    }

    @Test
    fun `skal returnere erUnderOppfolging - true når bruker ER under oppfølging`() {
        val (fnr, aktorId) = defaultBruker()
        setBrukerUnderOppfolging(aktorId)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getUnderOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "erUnderOppfolging": true }
        """.trimIndent())
    }

    @Test
    fun `skal returnere erUnderOppfolging - false når bruker ikke under oppfølging`() {
        val (fnr, _) = defaultBruker()

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getUnderOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "erUnderOppfolging": false }
        """.trimIndent())
    }
}
