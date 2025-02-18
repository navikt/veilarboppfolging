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
    ukjent
}

data class Folkeregisterpersonstatus(
    val forenkletStatus: String
)

data class HentFolkeregisterPersonStatus(
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>
)

data class HentFolkeregisterPersonStatusQuery(
    val hentPerson: HentFolkeregisterPersonStatus,
)

class HentFolkeregisterPersonStatusGraphqlWrapper: GraphqlResponse<HentFolkeregisterPersonStatusQuery>()

@Service
class PdlFolkeregisterStatusClient(val pdlClient: PdlClient) {
    init {
        JsonUtils.getMapper().registerKotlinModule()
    }

    val logger = LoggerFactory.getLogger(PdlFolkeregisterStatusClient::class.java)

    fun hentFolkeregisterStatus(fnr: Fnr): ForenkletFolkeregisterStatus {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/hentFolkeregisterStatus.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        val result = pdlClient.request(graphqlRequest, HentFolkeregisterPersonStatusGraphqlWrapper::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }

        if(result.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl ${result?.errors.toString()}") }

        val forenkletStatusString = result.data.hentPerson.folkeregisterpersonstatus.firstOrNull()?.forenkletStatus
        return when {
            (forenkletStatusString == ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.dNummer.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.doedIFolkeregisteret.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.ikkeBosatt.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.forsvunnet.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            (forenkletStatusString == ForenkletFolkeregisterStatus.opphoert.name) -> ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven
            else -> {
                logger.warn("Ukjent forenkletFolkeregisterStatus", forenkletStatusString)
                ForenkletFolkeregisterStatus.ukjent
            }
        }
    }
}
