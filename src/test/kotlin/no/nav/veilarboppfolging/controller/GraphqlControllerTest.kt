package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.veilarboppfolging.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

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

    @Test
    fun `skal returnere kanStarteOppfolging - JA når veileder har tilgang`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = Fnr.of("12444678910")
        val aktorId = AktorId.of("12444678919")
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "JA" }
        """.trimIndent())
    }

    @Test
    fun `skal returnere kanStarteOppfolging - ALLEREDE_UNDER_OPPFOLGING når bruker allerede under oppfølging`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = Fnr.of("12444678910")
        val aktorId = AktorId.of("12444678919")
        setBrukerUnderOppfolging(aktorId)
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "ALLEREDE_UNDER_OPPFOLGING" }
        """.trimIndent())

    }

    @Test
    fun `skal returnere kanStarteOppfolging - skal returnere hvorfor veileder ikke har tilgang`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = Fnr.of("12444678910")
        val aktorId = AktorId.of("12444678919")
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)

        listOf(
            AdGruppeNavn.FORTROLIG_ADRESSE to KanStarteOppfolging.IKKE_TILGANG_FORTROLIG_ADRESSE,
            AdGruppeNavn.MODIA_OPPFOLGING to KanStarteOppfolging.IKKE_TILGANG_MODIA,
            AdGruppeNavn.EGNE_ANSATTE to KanStarteOppfolging.IKKE_TILGANG_EGNE_ANSATTE,
            AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE to KanStarteOppfolging.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
            null to KanStarteOppfolging.IKKE_TILGANG_ENHET
        ).forEach { (adGruppe, kanStarteOppfolgingResult) ->
            mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Deny(
                message = "mangler tilgang til gruppe med navn ${adGruppe}",
                reason = "MANGLER_TILGANG_TIL_AD_GRUPPE"
            ))
            /* Query is hidden in test/resources/graphl-test :) */
            val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
            result.errors().verify()
            result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "$kanStarteOppfolgingResult" }
        """.trimIndent())
        }

    }
}
