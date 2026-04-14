package no.nav.veilarboppfolging.client.ungdomsprogram

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.function.Supplier

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

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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
