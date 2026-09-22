package no.nav.veilarboppfolging.client.aoKontor

import jakarta.ws.rs.core.HttpHeaders
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.function.Supplier

class AoKontorClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {

    fun hentForrigeAoKontor(fnr: Fnr): String {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/aoKontor/hentKontorHistorikk.graphql")
            .buildRequest(QueryVariables(ident = fnr.get()))
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/graphql"))
            .header(HttpHeaders.ACCEPT, RestUtils.MEDIA_TYPE_JSON.toString())
            .header(HttpHeaders.AUTHORIZATION, RestUtils.createBearerToken(tokenProvider.get()))
            .post( JsonUtils.toJson(graphqlRequest).toRequestBody(RestUtils.MEDIA_TYPE_JSON)).build()
        val kontorHistorikk = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hent ao-kontor historikk, status=${response.code}")
            }
            val rawBody = response?.body?.string() ?: throw RuntimeException("Klarte ikke å hent ao-kontor historikk, body var tom")
            val parsed =JsonUtils.fromJson(rawBody, AoKontorResponse::class.java)
            if(parsed.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til ao-kontor ${parsed.errors}") }
            parsed.data
        }
        if (kontorHistorikk == null) {
            throw RuntimeException("Klarte ikke å hent ao-kontorhistorikk, data var null")
        }
        return finnForrigeAoKontor(kontorHistorikk.kontorHistorikk)
    }

    private fun finnForrigeAoKontor(kontorHistorikk: List<KontorInnslag>): String {
        kontorHistorikk.firstOrNull { it.kontorType == KontorType.ARBEIDSOPPFOLGING }?.kontorId?.let { return it }

        kontorHistorikk.firstOrNull { it.kontorType == KontorType.ARENA }?.kontorId?.let { return it }

        throw RuntimeException("Fant ikke ao-kontor historikk for ARBEIDSOPPFOLGING")
    }
}

data class QueryVariables(
    val ident: String
)

enum class KontorType {
    ARBEIDSOPPFOLGING,
    ARENA,
    GEOGRAFISK_TILKNYTNING
}

data class KontorInnslag(
    val kontorId: String,
    val kontorType: KontorType,
)

data class KontorHistorikkData(
    val kontorHistorikk: List<KontorInnslag>
)

class AoKontorResponse: GraphqlResponse<KontorHistorikkData>() {}