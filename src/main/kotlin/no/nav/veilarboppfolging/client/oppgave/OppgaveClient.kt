package no.nav.veilarboppfolging.client.oppgave

import java.time.LocalDate
import java.util.UUID
import java.util.function.Supplier
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import okhttp3.HttpUrl.Companion.toHttpUrl
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
 * https://oppgave.dev.intern.nav.no/
 */
class OppgaveClient(
    baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient,
) {
    private val logger = LoggerFactory.getLogger(OppgaveClient::class.java)
    private val objectMapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private val apiPath = "$baseUrl/api/v1/oppgaver"
    private val mediaTypeJson = "application/json".toMediaType()

    fun opprettOppgave(
        fnr: Fnr,
        aktorId: AktorId,
    ): LocalDate {
        val request = OpprettOppgaveRequest(
            personident = fnr.get(),
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
        aktorId: AktorId,
        correlationId: String,
    ): FinnOppgaveResponse {
        val httpUrl = apiPath.toHttpUrl().newBuilder()
            .addQueryParameter("tema", TEMA_OPPFOLGING)
            .addQueryParameter("oppgavetype", OPPGAVETYPE_KONTAKT_BRUKER)
            .addQueryParameter("statuskategori", "AAPEN")
            .addQueryParameter("aktoerId", aktorId.get())
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
