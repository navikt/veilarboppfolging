package no.nav.veilarboppfolging.client.pdl

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Service

@Service
class GeografiskTilknytningClient(val pdlClient: PdlClient) {

    data class GeografiskTilknytningOgAdressebeskyttelse(
        val geografiskTilknytning: GeografiskTilknytningNr?,
        val strengtFortroligAdresse: Boolean
    )

    fun hentGeografiskTilknytning(fnr: Fnr): GeografiskTilknytningOgAdressebeskyttelse {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/hentGeografiskTilknytningOgAdressebeskyttelse.graphql")
            .buildRequest(QueryVariables(ident = fnr.get()))
        val result = pdlClient.request(graphqlRequest, GeografiskTilknytningOgAdresseBeskyttelseResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
        if(result.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl ${result?.errors.toString()}") }
        val strengtFortroligAdresse = result.data
            .let {
                when(it.adressebeskyttelse?.gradering) {
                    Gradering.STRENGT_FORTROLIG -> true
                    Gradering.FORTROLIG -> false
                    Gradering.STRENGT_FORTROLIG_UTLAND -> true
                    Gradering.UGRADERT -> false
                    else -> false
                }
            }
        val geografiskTilknytning = result.data
            .let {
                when (it.geografiskTilknytning.gtType) {
                    GTType.BYDEL -> it.geografiskTilknytning.gtBydel?.let { gtBydel -> GeografiskTilknytningNr(GTType.BYDEL, gtBydel) }
                    GTType.KOMMUNE -> it.geografiskTilknytning.gtKommune?.let { gtKommune -> GeografiskTilknytningNr(GTType.KOMMUNE, gtKommune) }
                    GTType.UTLAND -> it.geografiskTilknytning.gtLand?.let { gtLand -> GeografiskTilknytningNr(GTType.UTLAND, gtLand) }
                    else -> null
                }
            }
        return GeografiskTilknytningOgAdressebeskyttelse(geografiskTilknytning, strengtFortroligAdresse)

    }
}

data class GeografiskTilknytningNr (
    val gtType: GTType,
    val nr: String,
)

data class QueryVariables(
    val ident: String,
    val historikk: Boolean = false
)

enum class GTType {
    BYDEL, KOMMUNE, UDEFINERT, UTLAND
}

data class GeografiskTilknytningOgAdressebeskyttelse(
    val geografiskTilknytning: GeografiskTilknytning,
    val adressebeskyttelse: Adressebeskyttelse?
)

data class GeografiskTilknytning(
    val gtType: GTType,
    val gtKommune: String?,
    val gtBydel: String?,
    val gtLand: String?,
)

enum class Gradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    UGRADERT
}

data class Adressebeskyttelse(
    val gradering: Gradering
)

class GeografiskTilknytningOgAdresseBeskyttelseResponse: GraphqlResponse<GeografiskTilknytningOgAdressebeskyttelse>()