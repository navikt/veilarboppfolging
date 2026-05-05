package no.nav.veilarboppfolging.client.ungdomsprogram

import java.util.function.Supplier
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

/**
 * Klient for å sjekke deltakelse i ungdomsprogrammet.
 * Dokumentasjon: https://ung-deltakelse-opplyser.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=ekstern
 */
class UngdomsprogramClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val objectMapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private val mediaTypeJson = "application/json".toMediaType()

    fun erDeltakerIUngdomsprogrammet(personident: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/ekstern/deltakelse/sjekk")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(
                objectMapper.writeValueAsString(SjekkDeltakelseRequest(personident))
                    .toRequestBody(mediaTypeJson)
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å sjekke deltakelse i ungdomsprogrammet, status=${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Body mangler i respons fra ung-deltakelse-opplyser")

            val sjekkDeltakelseResponse = objectMapper.readValue<SjekkDeltakelseResponse>(body)
            logger.info("Sjekket deltakelse i ungdomsprogrammet, erDeltaker=${sjekkDeltakelseResponse.erDeltaker}")
            return sjekkDeltakelseResponse.erDeltaker
        }
    }
}

data class SjekkDeltakelseRequest(val deltakerIdent: String)

data class SjekkDeltakelseResponse(
    val erDeltaker: Boolean,
    val fraOgMed: java.time.LocalDate?,
    val tilOgMed: java.time.LocalDate?,
)
