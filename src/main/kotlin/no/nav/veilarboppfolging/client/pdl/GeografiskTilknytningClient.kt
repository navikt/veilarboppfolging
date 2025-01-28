package no.nav.veilarboppfolging.client.pdl

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Service

@Service
class GeografsiskTilknytningClient(val pdlClient: PdlClient) {

    fun hentGeografiskTilknytning(fnr: Fnr): GeografiskTilknytningNr? {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/hentGeografiskTilknytning.graphql")
            .buildRequest(QueryVariables(ident = fnr.get()))
        val gtResult = pdlClient.request(graphqlRequest, GeografiskTilknytningResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
        if(gtResult.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl") }
        return gtResult.data
            .let {
                when (it.gtType) {
                    GTType.BYDEL -> it.gtBydel?.let { gtBydel -> GeografiskTilknytningNr(GTType.BYDEL, gtBydel) }
                    GTType.KOMMUNE -> it.gtKommune?.let { gtKommune -> GeografiskTilknytningNr(GTType.KOMMUNE, gtKommune) }
                    else -> null
                }
            }
    }
}

data class GeografiskTilknytningNr (
    val gtType: GTType,
    val nr: String,
)

data class QueryVariables(
    val ident: String,
)

enum class GTType {
    BYDEL, KOMMUNE, UDEFINERT, UTLAND
}

data class GeografiskTilknytning(
    val gtType: GTType,
    val gtKommune: String?,
    val gtBydel: String?,
    val gtLand: String?,
)

class GeografiskTilknytningResponse: GraphqlResponse<GeografiskTilknytning>()