package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.controller.graphql.AdGruppeNavn
import no.nav.veilarboppfolging.ident.randomAktorId
import no.nav.veilarboppfolging.ident.randomFnr
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestClient
import java.util.UUID
import kotlin.test.assertEquals

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

    val norskStatsborgerskap = listOf("NOR")

    fun defaultBruker(): Pair<Fnr, AktorId> {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockSytemBrukerAuthOk(aktorId, fnr)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven, norskStatsborgerskap))
        return fnr to aktorId
    }

    @Test
    fun `graphql endepunkt skal finnes på veilarboppfolging api graphql`() {
        val apiUrl = "http://localhost:${port}/veilarboppfolging/api/graphql"
        val graphqlClient = RestClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Nav-Call-Id", "test")
            .build()

        val pesos = '$'
        val fnrparam = "${pesos}fnr"

        val graphqlQuery = """
            query testHentOppfolgingsEnhet(${fnrparam}: String!) {
                gjeldendeOppfolgingsperiode(fnr: ${fnrparam}) {
                    id
                }
            }
        """.trimIndent().replace("\n", "")

        val status = graphqlClient.post()
            .uri(apiUrl)
            .body("""{
                "query": "$graphqlQuery",
                "variables": {
                    "fnr": "123123123"
                }
            }
            """.trimIndent().replace("\n", ""))
            .retrieve()
            .toBodilessEntity()
            .statusCode

        assertEquals(HttpStatusCode.valueOf(200), status, "Fikk $status på /graphql istedetfor 200")
//            .body(OppfolgingDto::class.java)
    }

    @Test
    fun `skal returnere oppfolgingsEnhet`() {
        val (fnr, aktorId) = defaultBruker()
        val kontor = "7414"
        val kontorNavn = "Nav Graphql Kontor"
        val skjermet = false
        val veilederUuid = UUID.randomUUID()
        mockPdlGeografiskTilknytning(fnr, kontor)
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
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
    fun `skal returnere oppfolgingsEnhet ved spørring med aktorId som param istedetfor fnr`() {
        val (fnr, aktorId) = defaultBruker()
        val kontor = "7414"
        val kontorNavn = "Nav Graphql Kontor"
        val skjermet = false
        val veilederUuid = UUID.randomUUID()
        mockPdlGeografiskTilknytning(fnr, kontor)
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPoaoTilgangTilgangsAttributter(kontor, skjermet)
        mockNorgEnhetsNavn(kontor, kontorNavn)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", aktorId.get()).execute()
        result.errors().verify()
        result.path("oppfolgingsEnhet.enhet").matchesJson("""
            { "id": "${kontor}", "kilde": "NORG", "navn": "${kontorNavn}" }
        """.trimIndent())
    }

    @Test
    fun `skal returnere ukjent oppfolgingsenhet hvis enhet mangler i norg`() {
        val (fnr, aktorId) = defaultBruker()
        val kontor = "7414"
        val skjermet = false
        val veilederUuid = UUID.randomUUID()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPdlGeografiskTilknytning(fnr, kontor)
        mockPoaoTilgangTilgangsAttributter(kontor, skjermet)
       // mockNorgEnhetsNavn(kontor, kontorNavn)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getEnhetQuery").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolgingsEnhet.enhet").matchesJson("""
            { "id": "${kontor}", "kilde": "NORG", "navn": "Ukjent enhet" }
        """.trimIndent())
    }

    @Test
    fun `skal returnere error på oppfolgingsEnhet når noe skjer`() {
        val (fnr, aktorId) = defaultBruker()
        val veilederUuid = UUID.randomUUID()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
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
        val veilederUuid = UUID.randomUUID()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        setBrukerUnderOppfolging(aktorId, fnr)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getUnderOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "erUnderOppfolging": true }
        """.trimIndent())
    }

    @Test
    fun `skal returnere erUnderOppfolging - false når bruker ikke under oppfølging`() {
        val (fnr, aktorId) = defaultBruker()
        val veilederUuid = UUID.randomUUID()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)

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
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven, norskStatsborgerskap))
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "JA" }
        """.trimIndent())
    }

    @Test
    fun `skal returnere kanStarteOppfolging - ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT når bruker allerede under oppfølging men ISERV+kanReaktiveres i Arena`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        setBrukerUnderOppfolging(aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven, norskStatsborgerskap))
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockVeilarbArenaOppfolgingsStatus(fnr= fnr, formidlingsgruppe = Formidlingsgruppe.ISERV, kanEnkeltReaktiveres = true)
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT" }
        """.trimIndent())

    }

    @Test
    fun `skal returnere kanStarteOppfolging - ALLEREDE_UNDER_OPPFOLGING når bruker allerede under oppfølging`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        setBrukerUnderOppfolging(aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven, norskStatsborgerskap))
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "ALLEREDE_UNDER_OPPFOLGING" }
        """.trimIndent())

    }

    @Test
    fun `skal ikke kalle pdl for bosattstatus dersom veileder ikke tilgang til bruker`() {
        // Kallet mot pdl vil feile dersom det blir kalt for en bruker med diskresjonskode hvis veileder ikke har spesielle rettigheter
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
       // setBrukerUnderOppfolging(aktorId)
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Deny(
            message = "mangler tilgang til gruppe med navn ${AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE}",
            reason =  "MANGLER_TILGANG_TIL_AD_GRUPPE"
        ))
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE" }
        """.trimIndent())
        verifyNoInteractions(pdlFolkeregisterStatusClient)
    }

    @Test
    fun `skal returnere kanStarteOppfolging - skal returnere hvorfor veileder ikke har tilgang`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)

        listOf(
            AdGruppeNavn.FORTROLIG_ADRESSE to KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE,
            AdGruppeNavn.MODIA_OPPFOLGING to KanStarteOppfolgingDto.IKKE_TILGANG_MODIA,
            AdGruppeNavn.EGNE_ANSATTE to KanStarteOppfolgingDto.IKKE_TILGANG_EGNE_ANSATTE,
            AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE to KanStarteOppfolgingDto.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
            null to KanStarteOppfolgingDto.IKKE_TILGANG_ENHET
        ).forEach { (adGruppe, kanStarteOppfolgingResult) ->
            mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Deny(
                message = "mangler tilgang til gruppe med navn ${adGruppe}",
                reason = if (adGruppe != null) "MANGLER_TILGANG_TIL_AD_GRUPPE" else "IKKE_TILGANG_TIL_NAV_ENHET"
            ))
            /* Query is hidden in test/resources/graphl-test :) */
            val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
            result.errors().verify()
            result.path("oppfolging").matchesJson("""
            { "kanStarteOppfolging": "$kanStarteOppfolgingResult" }
        """.trimIndent())
        }
    }

    @Test
    fun `skal returnere kanStarteOppfolging - norsk statsborger - skal returnere hvorfor veileder starte oppfølging på brukere som har feil status i freg`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)

        listOf(
            ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven to KanStarteOppfolgingDto.JA,
            ForenkletFolkeregisterStatus.dNummer to KanStarteOppfolgingDto.JA,
            ForenkletFolkeregisterStatus.doedIFolkeregisteret to KanStarteOppfolgingDto.DOD,
            ForenkletFolkeregisterStatus.forsvunnet to KanStarteOppfolgingDto.IKKE_LOVLIG_OPPHOLD,
            ForenkletFolkeregisterStatus.ikkeBosatt to KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
            ForenkletFolkeregisterStatus.opphoert to KanStarteOppfolgingDto.IKKE_LOVLIG_OPPHOLD,
            ForenkletFolkeregisterStatus.ukjent to KanStarteOppfolgingDto.UKJENT_STATUS_FOLKEREGISTERET,
            ForenkletFolkeregisterStatus.ingen_status to KanStarteOppfolgingDto.INGEN_STATUS_FOLKEREGISTERET,
        ).forEach { (status, kanStarteOppfolgingResult) ->
            mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(status, norskStatsborgerskap))
            /* Query is hidden in test/resources/graphl-test :) */
            val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
            result.errors().verify()
            result.path("oppfolging").matchesJson("""
                { "kanStarteOppfolging": "$kanStarteOppfolgingResult" }
            """.trimIndent())
        }
    }

    @Test
    fun `skal returnere kanStarteOppfolging - tredjelandsborger - skal returnere hvorfor veileder starte oppfølging på brukere som har feil status i freg`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        val tredjelandsStatsborgerskap = listOf("RUS")
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)

        listOf(
            ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven to KanStarteOppfolgingDto.JA,
            ForenkletFolkeregisterStatus.dNummer to KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR,
            ForenkletFolkeregisterStatus.doedIFolkeregisteret to KanStarteOppfolgingDto.DOD,
            ForenkletFolkeregisterStatus.forsvunnet to KanStarteOppfolgingDto.IKKE_LOVLIG_OPPHOLD,
            ForenkletFolkeregisterStatus.ikkeBosatt to KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
            ForenkletFolkeregisterStatus.opphoert to KanStarteOppfolgingDto.IKKE_LOVLIG_OPPHOLD,
            ForenkletFolkeregisterStatus.ukjent to KanStarteOppfolgingDto.UKJENT_STATUS_FOLKEREGISTERET,
            ForenkletFolkeregisterStatus.ingen_status to KanStarteOppfolgingDto.INGEN_STATUS_FOLKEREGISTERET,
        ).forEach { (status, kanStarteOppfolgingResult) ->
            mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(status, tredjelandsStatsborgerskap))
            /* Query is hidden in test/resources/graphl-test :) */
            val result = tester.documentName("kanStarteOppfolging").variable("fnr", fnr.get()).execute()
            result.errors().verify()
            result.path("oppfolging").matchesJson("""
                { "kanStarteOppfolging": "$kanStarteOppfolgingResult" }
            """.trimIndent())
        }
    }

    @Test
    fun `skal ha tilgang til brukers sjekke veileders tilganger til bruker`() {
        val veilederUuid = UUID.randomUUID()
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        val enhetId = EnhetId("3131")
        setBrukerUnderOppfolging(aktorId, fnr)
        setBrukerUnderKvp(aktorId, enhetId.get(), veilederUuid.toString())
        mockInternBrukerAuthOk(veilederUuid, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederUuid, fnr, Decision.Permit)
        mockPoaoTilgangHarTilgangTilEnhet(veilederUuid, enhetId, Decision.Deny("NEI", "FORDI"))
        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("veilederTilganger").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("veilederLeseTilgangModia").matchesJson("""
            { 
                "harVeilederLeseTilgangTilBruker": true,
                "harVeilederLeseTilgangTilBrukersKontorsperre": false,
                "tilgang": "HAR_TILGANG"
            }
        """.trimIndent())
        result.path("brukerStatus").matchesJson("""
            { 
                "kontorSperre": {
                    "kontorId": "${enhetId.get()}"
                }
            }
        """.trimIndent())
    }

    @Test
    fun `skal returnere oppfolgingsperiodene til bruker`() {
        val veilederId = UUID.randomUUID()
        val (fnr, aktorId) = defaultBruker()
        mockInternBrukerAuthOk(veilederId, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederId, fnr, Decision.Permit)
        setBrukerUnderOppfolging(aktorId, fnr)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("getOppfolgingsperioder").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("oppfolgingsPerioder").matchesJson("""
            [ { sluttTidspunkt: null } ]
        """.trimIndent())
    }

    @Test
    fun `skal returnere manuell status og reservert på bruker`() {
        val veilederId = UUID.randomUUID()
        val (fnr, aktorId) = defaultBruker()
        mockInternBrukerAuthOk(veilederId, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederId, fnr, Decision.Permit)
        setBrukerUnderOppfolging(aktorId, fnr)
        setLocalArenaOppfolging(aktorId)
        setManuellStatus(aktorId)
        mockDigdir(fnr, reservertMotDigitalKommunikasjon = true, kanVarsles = false)

        /* Query is hidden in test/resources/graphl-test :) */
        val result = tester.documentName("hentBrukerStatus").variable("fnr", fnr.get()).execute()
        result.errors().verify()
        result.path("brukerStatus").matchesJson(
            """
            { 
              "manuell": {
                "erManuell": true,
                "begrunnelse": "Fordi",
                "endretAvType": "NAV",
                "endretAvIdent": "A112211"
              },
              "kontorSperre": null,
              "erKontorsperret": false,
              "krr": {
                  "registrertIKrr": true,
                  "reservertIKrr": true,
                  "kanVarsles": false
              },
              "arena": {
                "inaktivIArena": false,
                "kanReaktiveres": null,
                "inaktiveringsdato": null,
                "kvalifiseringsgruppe": "VURDI"
              }
            }
        """.trimIndent())
    }
}
