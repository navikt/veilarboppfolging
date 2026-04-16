package no.nav.veilarboppfolging.client.oppgave

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDate
import java.util.UUID
import java.util.function.Supplier
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

/**
 * https://oppgave.dev.intern.nav.no/
 */
class OppgaveClient(
    baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {
    private val logger = LoggerFactory.getLogger(OppgaveClient::class.java)
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val apiPath = "$baseUrl/api/v1/oppgaver"
    private val mediaTypeJson = "application/json".toMediaType()

    fun opprettOppgave(
        fnr: String,
        aktorId: String,
    ): LocalDate {
        val request = OpprettOppgaveRequest(
            personident = fnr,
        )
        val correlationId = UUID.randomUUID().toString()
        val oppgaveResponse = finnOppgave(
            aktorId = aktorId,
            correlationId = correlationId,
        )
        if (oppgaveResponse.antallTreffTotalt > 0 && oppgaveResponse.oppgaver.any { it.fristFerdigstillelse != null }) {
            logger.info("Det finnes allerede en åpen kontakt bruker-oppgave, correlationId: $correlationId")
            val frist = oppgaveResponse.oppgaver
                .filter { it.fristFerdigstillelse != null }
                .sortedBy { it.fristFerdigstillelse }
                .first().fristFerdigstillelse!!
            return frist
        }
        val opprettOppgaveResponse = opprettOppgave(
            opprettOppgaveRequest = request,
            correlationId = correlationId,
        )
        logger.info("Opprettet oppgave med id ${opprettOppgaveResponse.id}, correlationId: $correlationId")
        return opprettOppgaveResponse.fristFerdigstillelse!!
    }

    private fun opprettOppgave(
        opprettOppgaveRequest: OpprettOppgaveRequest,
        correlationId: String,
    ): OppgaveResponse {
        val request = Request.Builder()
            .url(apiPath)
            .addHeader("X-Correlation-ID", correlationId)
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .post(objectMapper.writeValueAsString(opprettOppgaveRequest).toRequestBody(mediaTypeJson))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("Noe gikk galt ved oppretting oppgave: ${response.code}, correlationId: $correlationId")
                throw RuntimeException("Oppgave svarte med feilmelding ved oppretting av oppgave: ${response.code}")
            }

            val body = response.body?.string() ?: throw RuntimeException("Body is missing")
            return objectMapper.readValue<OppgaveResponse>(body)
        }
    }

    private fun finnOppgave(
        aktorId: String,
        correlationId: String,
    ): FinnOppgaveResponse {
        val httpUrl = apiPath.toHttpUrl().newBuilder()
            .addQueryParameter("tema", TEMA_OPPFOLGING)
            .addQueryParameter("oppgavetype", OPPGAVETYPE_KONTAKT_BRUKER)
            .addQueryParameter("statuskategori", "AAPEN")
            .addQueryParameter("aktoerId", aktorId)
            .build()
        logger.info(httpUrl.toString())
        val request = Request.Builder()
            .url(httpUrl)
            .addHeader("X-Correlation-ID", correlationId)
            .addHeader("Authorization", "Bearer ${tokenProvider.get()}")
            .get().build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("Noe gikk galt ved duplikatsjekk mot oppgave: ${response.code}, correlationId: $correlationId")
                throw RuntimeException("Oppgave svarte med feilmelding ved duplikatsjekk: ${response.code}")
            }

            val body = response.body?.string() ?: throw RuntimeException("Body is missing")
            return objectMapper.readValue<FinnOppgaveResponse>(body)
        }
    }
}
