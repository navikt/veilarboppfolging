package no.nav.veilarboppfolging.client.amtdeltaker

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
import java.util.function.Supplier

class AmtDeltakerClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val mediaTypeJson = "application/json".toMediaType()

    fun harAktiveTiltaksdeltakelser(personident: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/external/aktiv-deltaker")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(objectMapper.writeValueAsString(HentDeltakelserRequest(personident)).toRequestBody(mediaTypeJson))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente tiltaksdeltakelser fra amt-deltaker, status=${response.code}")
            }

            val body = response.body?.string() ?: throw RuntimeException("Body mangler i respons fra amt-deltaker")

            return objectMapper.readValue<HarAktiveDeltakelserResponse>(body).harAktiveDeltakelser
        }
    }
}
