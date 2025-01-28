package no.nav.veilarboppfolging.client.norg

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarboppfolging.VeilarboppfolgingException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

open class NorgTilhorighetClient(
	private val baseUrl: String,
	private val httpClient: OkHttpClient = RestClient.baseClient(),
) : INorgTilhorighetClient {

	private val logger = LoggerFactory.getLogger(VeilarboppfolgingException::class.java)

	override fun hentTilhorendeEnhet(norgTilhorighetRequest: NorgTilhorighetRequest): Enhet? {

		val httpUrl = joinPaths(baseUrl, "/norg2/api/v1/enhet/navkontor/", norgTilhorighetRequest.geografiskTilknytning.nr).toHttpUrl().newBuilder()
			.addQueryParameter("skjermet", norgTilhorighetRequest.skjermet.toString())
			.addQueryParameter("fortroligAdresse", if (norgTilhorighetRequest.fortroligAdresse) "SPSF" else "")
			.build()
		val request = Request.Builder().url(httpUrl).get().build()

		httpClient.newCall(request).execute().use { response ->

			if (response.code == 404) {
				logger.warn("Fant ikke NAV-enhet basert på geografisk tilknytning")
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException(
					"Klarte ikke å hente NAV-enhet basert på geografisk tilknytning = ${norgTilhorighetRequest.geografiskTilknytning.nr} fra Norg. Status: ${response.code}"
				)
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")
			val enhetResponse = JsonUtils.fromJson(body, EnhetResponse::class.java)
			return Enhet(enhetResponse.enhetNr, enhetResponse.enhetNavn)
		}
	}

	private data class EnhetResponse(val enhetNr: String, val enhetNavn: String)
}
