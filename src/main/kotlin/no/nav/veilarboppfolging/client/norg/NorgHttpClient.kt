package no.nav.veilarboppfolging.client.norg

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarboppfolging.VeilarboppfolgingException
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

open class NorgHttpClient(
	private val baseUrl: String,
	private val httpClient: OkHttpClient = RestClient.baseClient(),
) : NorgClient {

	private val logger = LoggerFactory.getLogger(VeilarboppfolgingException::class.java)

	override fun hentTilhorendeEnhet(
		geografiskTilknytning: GeografiskTilknytningNr,
	): EnhetId? {

		val requestBuilder = Request.Builder()
			.url(joinPaths(baseUrl, "/norg2/api/v1/enhet/navkontor/", geografiskTilknytning.nr))
			.get()

		val request = requestBuilder.build()

		httpClient.newCall(request).execute().use { response ->

			if (response.code == 404) {
				logger.warn("Fant ikke NAV-enhet basert på geografisk tilknytning")
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException(
					"Klarte ikke å hente NAV-enhet basert på geografisk tilknytning = $geografiskTilknytning fra Norg. Status: ${response.code}"
				)
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")
			val enhetResponse = JsonUtils.fromJson(body, EnhetResponse::class.java)
			return enhetResponse.enhetNr.let { EnhetId.of(it) }
		}
	}

	private data class EnhetResponse(val enhetNr: String)
}
