package no.nav.veilarboppfolging.client.aoKontor

import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningOgAdresseBeskyttelseResponse
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class AoKontorClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {

    fun hentForrigeAoKontor(fnr: Fnr) {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/hentGeografiskTilknytningOgAdressebeskyttelse.graphql")
            .buildRequest(QueryVariables(ident = fnr.get()))
        val result = aoKontorHttpClient.request(graphqlRequest, GeografiskTilknytningOgAdresseBeskyttelseResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
    }

}

data class QueryVariables(
    val ident: String
)