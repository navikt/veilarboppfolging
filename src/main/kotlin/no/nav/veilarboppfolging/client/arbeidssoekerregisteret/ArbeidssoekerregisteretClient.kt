package no.nav.veilarboppfolging.client.arbeidssoekerregisteret

import java.time.Instant
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
 * Klient for å sjekke om en person er registrert som aktiv arbeidssøker i arbeidssøkerregisteret.
 * Dokumentasjon: https://oppslag-v2-arbeidssoekerregisteret.intern.dev.nav.no/api/docs/v3
 */
class ArbeidssoekerregisteretClient(
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

    fun erArbeidssoeker(personident: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/api/v3/perioder")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(
                objectMapper.writeValueAsString(ArbeidssoekerperioderRequest(personident))
                    .toRequestBody(mediaTypeJson)
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å sjekke arbeidssøkerstatus, status=${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Body mangler i respons fra arbeidssøkerregisteret")

            val perioder = objectMapper.readValue<List<ArbeidssoekerperiodeResponse>>(body)
            val erAktivArbeidssoeker = perioder.any { it.avsluttet == null }
            logger.info("Sjekket arbeidssøkerstatus, erArbeidssoeker=$erAktivArbeidssoeker, antallPerioder=${perioder.size}")
            return erAktivArbeidssoeker
        }
    }
}

data class ArbeidssoekerperioderRequest(val identitetsnummer: String) {
    val type: String = "IDENTITETSNUMMER"
}

data class ArbeidssoekerperiodeResponse(
    val periodeId: String,
    val startet: Instant,
    val avsluttet: Instant?,
)
