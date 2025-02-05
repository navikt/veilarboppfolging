package no.nav.veilarboppfolging.client.pdl

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Service


@Service
class GeografiskTilknytningClient(val pdlClient: PdlClient) {

    init {
        JsonUtils.getMapper().registerKotlinModule()
    }

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
                when(it.hentPerson.adressebeskyttelse.firstOrNull()?.gradering) {
                    Gradering.STRENGT_FORTROLIG -> true
                    Gradering.FORTROLIG -> false
                    Gradering.STRENGT_FORTROLIG_UTLAND -> true
                    Gradering.UGRADERT -> false
                    else -> false
                }
            }
        val geografiskTilknytning = result.data
            .let {
                when (it.hentGeografiskTilknytning?.gtType) {
                    GTType.BYDEL -> it.hentGeografiskTilknytning.gtBydel?.let { gtBydel -> GeografiskTilknytningNr(GTType.BYDEL, gtBydel) }
                    GTType.KOMMUNE -> it.hentGeografiskTilknytning.gtKommune?.let { gtKommune -> GeografiskTilknytningNr(GTType.KOMMUNE, gtKommune) }
                    GTType.UTLAND -> it.hentGeografiskTilknytning.gtLand?.let { gtLand -> GeografiskTilknytningNr(GTType.UTLAND, gtLand) }
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

data class HentPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>
)

data class PdlResponse(
    val hentGeografiskTilknytning: GeografiskTilknytning,
    val hentPerson: HentPerson
)

class GeografiskTilknytningOgAdresseBeskyttelseResponse: GraphqlResponse<PdlResponse>()