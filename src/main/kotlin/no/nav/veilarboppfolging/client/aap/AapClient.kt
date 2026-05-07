package no.nav.veilarboppfolging.client.aap

import java.time.LocalDate
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
 * Klient for å sjekke om en person har ytelsen AAP (arbeidsavklaringspenger).
 * Dokumentasjon: https://aap-api.intern.dev.nav.no/swagger-ui/index.html#/ArenaHistorikk/post_arena_person_saker
 */
class AapClient(
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

    fun harAap(personident: String): Boolean {
        // Endepunktet krever et datointervall - bruker tidenes morgen og langt inn i fremtiden
        // for å fange opp alle perioder.
        val requestBody = AapPerioderRequest(
            fraOgMedDato = LocalDate.of(1970, 1, 1),
            personidentifikator = personident,
            tilOgMedDato = LocalDate.of(9999, 12, 31),
        )

        val request = Request.Builder()
            .url("$baseUrl/perioder")
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(
                objectMapper.writeValueAsString(requestBody)
                    .toRequestBody(mediaTypeJson)
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å sjekke AAP-status, status=${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Body mangler i respons fra aap-api")

            val perioder = objectMapper.readValue<AapPerioderResponse>(body).perioder
            val idag = LocalDate.now()
            val harAktivAap = perioder.any { !it.tilOgMedDato.isBefore(idag) }
            logger.info("Sjekket AAP-status, harAap=$harAktivAap, antallPerioder=${perioder.size}")
            return harAktivAap
        }
    }
}

data class AapPerioderRequest(
    val fraOgMedDato: LocalDate,
    val personidentifikator: String,
    val tilOgMedDato: LocalDate,
)

data class AapPerioderResponse(
    val perioder: List<AapPeriode>,
)

data class AapPeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
)

