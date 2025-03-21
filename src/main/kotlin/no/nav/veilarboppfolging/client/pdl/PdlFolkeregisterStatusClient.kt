package no.nav.veilarboppfolging.client.pdl

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class ForenkletFolkeregisterStatus {
    bosattEtterFolkeregisterloven,
    ikkeBosatt,
    forsvunnet,
    doedIFolkeregisteret,
    opphoert,
    dNummer,
    ingen_status,
    ukjent
}

data class Folkeregisterpersonstatus(
    val forenkletStatus: String,
)

data class Statsborgerskap(
    val land: String
)

data class HentFolkeregisterPersonStatus(
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    val statsborgerskap: List<Statsborgerskap>
)

data class HentFolkeregisterPersonStatusQuery(
    val hentPerson: HentFolkeregisterPersonStatus,
)

typealias StatsborgerskapLand = String

data class FregStatusOgStatsborgerskap(
    val fregStatus: ForenkletFolkeregisterStatus,
    val statsborgerskap: List<StatsborgerskapLand>
)

class HentFolkeregisterPersonStatusGraphqlWrapper: GraphqlResponse<HentFolkeregisterPersonStatusQuery>()

@Service
class PdlFolkeregisterStatusClient(val pdlClient: PdlClient) {
    init {
        JsonUtils.getMapper().registerKotlinModule()
    }

    val logger = LoggerFactory.getLogger(PdlFolkeregisterStatusClient::class.java)


    fun hentFolkeregisterStatus(fnr: Fnr): FregStatusOgStatsborgerskap {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/hentFolkeregisterStatus.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        val result = pdlClient.request(graphqlRequest, HentFolkeregisterPersonStatusGraphqlWrapper::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }

        if(result.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl ${result?.errors.toString()}") }

        return FregStatusOgStatsborgerskap(
            getFregStatus(result),
            result.data.hentPerson.statsborgerskap.map { it.land }
        )
    }

    private fun getFregStatus(result: HentFolkeregisterPersonStatusGraphqlWrapper): ForenkletFolkeregisterStatus {
        val forenkletStatusString = result.data.hentPerson.folkeregisterpersonstatus.firstOrNull()?.forenkletStatus
        return when {
            (forenkletStatusString == ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.dNummer.name) -> ForenkletFolkeregisterStatus.dNummer
            (forenkletStatusString == ForenkletFolkeregisterStatus.doedIFolkeregisteret.name) -> ForenkletFolkeregisterStatus.doedIFolkeregisteret
            (forenkletStatusString == ForenkletFolkeregisterStatus.ikkeBosatt.name) -> ForenkletFolkeregisterStatus.ikkeBosatt
            (forenkletStatusString == ForenkletFolkeregisterStatus.forsvunnet.name) -> ForenkletFolkeregisterStatus.forsvunnet
            (forenkletStatusString == ForenkletFolkeregisterStatus.opphoert.name) -> ForenkletFolkeregisterStatus.opphoert
            result.data.hentPerson.folkeregisterpersonstatus.isEmpty() -> ForenkletFolkeregisterStatus.ingen_status
            else -> {
                logger.warn("Ukjent forenkletFolkeregisterStatus", forenkletStatusString)
                ForenkletFolkeregisterStatus.ukjent
            }
        }
    }
}
