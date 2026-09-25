package no.nav.veilarboppfolging.client.isoppfolgingstilfelle

import java.time.LocalDate
import java.util.function.Supplier
import no.nav.common.rest.client.RestUtils
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

class IsOppfolgingstilfelleClient(
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

    fun hentStatus(personident: String): OppfolgingstilfelleStatus? {
        val request = Request.Builder()
            .url("$baseUrl/api/system/v1/oppfolgingstilfelle/personident")
            .addHeader("Authorization", RestUtils.createBearerToken(tokenProvider.get()))
            .post(
                objectMapper.writeValueAsString(PersonIdentRequest(personident))
                    .toRequestBody(mediaTypeJson)
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) {
                return null
            }

            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente oppfolgingstilfelle, status=${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Body mangler i respons fra isoppfolgingstilfelle")

            val dto = objectMapper.readValue<OppfolgingstilfellePersonDTO>(body)
            val status = dto.utledStatus()
            logger.info("Sjekket oppfolgingstilfelle, status=$status, antallTilfeller=${dto.oppfolgingstilfelleList.size}")
            return status
        }
    }
}

data class PersonIdentRequest(
    val personIdent: String,
)

enum class OppfolgingstilfelleStatus {
    SYKMELDT_MED_ARBEIDSGIVER,
    SYKMELDT_UTEN_ARBEIDSGIVER,
}

data class OppfolgingstilfellePersonDTO(
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
    val personIdent: String,
    val dodsdato: LocalDate?,
    val hasGjentakendeSykefravar: Boolean?,
)

data class OppfolgingstilfelleDTO(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val varighetUker: Int,
    val virksomhetsnummerList: List<String>,
)

private fun OppfolgingstilfellePersonDTO.utledStatus(): OppfolgingstilfelleStatus? {
    val aktivtTilfelle = oppfolgingstilfelleList
        .filter { !it.end.isBefore(LocalDate.now()) }
        .maxByOrNull { it.start }
        ?: return null

    return if (aktivtTilfelle.arbeidstakerAtTilfelleEnd) {
        OppfolgingstilfelleStatus.SYKMELDT_MED_ARBEIDSGIVER
    } else {
        OppfolgingstilfelleStatus.SYKMELDT_UTEN_ARBEIDSGIVER
    }
}
