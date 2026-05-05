package no.nav.veilarboppfolging.client.tiltakshistorikk

import java.util.function.Supplier
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

// Dokumentasjon: https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-tiltakshistorikk
class TiltakshistorikkClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val objectMapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private val mediaTypeJson = "application/json".toMediaType()

    fun harAktiveTiltaksdeltakelser(personident: String): Boolean {
        val tiltakshistorikk = hentTiltaksdeltakelser(personident)
        logger.info("Fant ${tiltakshistorikk.size} tiltaksdeltakelser")
        val aktiveTiltaksdeltakelser = tiltakshistorikk.filter {
            it.erAktiv()
        }
        logger.info("Fant ${aktiveTiltaksdeltakelser.size} aktive tiltaksdeltakelser")
        return aktiveTiltaksdeltakelser.isNotEmpty()
    }

    private fun hentTiltaksdeltakelser(personident: String): List<TiltakshistorikkV1Dto> {
        val response = hentTiltakshistorikk(personident)

        if (response.kunneIkkeHenteDeltakelserFraTeamTiltak()) {
            logger.warn("Tiltakshistorikk kunne ikke hente tiltak fra Team Tiltak. Prøver på nytt..")
            val retryResponse = hentTiltakshistorikk(personident)

            if (retryResponse.kunneIkkeHenteDeltakelserFraTeamTiltak()) {
                logger.warn("Tiltakshistorikk kunne ikke hente tiltak fra Team Tiltak med retry, returnerer responsen")
            }
            return retryResponse.historikk
        } else {
            return response.historikk
        }
    }

    private fun hentTiltakshistorikk(personident: String): TiltakshistorikkResponse {
        val request = Request.Builder()
            .url("$baseUrl/api/v1/historikk")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(objectMapper.writeValueAsString(TiltakshistorikkRequest(listOf(NorskIdent(personident)))).toRequestBody(mediaTypeJson))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente tiltaksdeltakelser fra tiltakshistorikk, status=${response.code}")
            }

            val body = response.body?.string() ?: throw RuntimeException("Body mangler i respons fra tiltakshistorikk")

            return objectMapper.readValue<TiltakshistorikkResponse>(body)
        }
    }
}