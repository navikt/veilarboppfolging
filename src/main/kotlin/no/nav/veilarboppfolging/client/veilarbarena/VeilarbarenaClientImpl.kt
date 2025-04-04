package no.nav.veilarboppfolging.client.veilarbarena

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import no.nav.veilarboppfolging.service.AuthService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*


sealed class TokenResult {
    class Success(val token: String): TokenResult()
    class Fail(val message: String, val reason: Throwable): TokenResult()
}
sealed class RequestResult<T> {
    class Success<T>(val body: Optional<T>): RequestResult<T>()
    class Fail<T>(val message: String, val reason: Throwable): RequestResult<T>()
}

@Slf4j
class VeilarbarenaClientImpl(
    private val veilarbarenaUrl: String,
    private val veilarbarenaAadTokenScope: String,
    private val authService: AuthService
) : VeilarbarenaClient {
    private val client: OkHttpClient = RestClient.baseClient()
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        JsonUtils.getMapper().registerKotlinModule()
    }

    private fun getToken(): TokenResult {
        return runCatching {
            when {
                authService.erInternBruker() -> authService.getAadOboTokenForTjeneste(veilarbarenaAadTokenScope)
                else -> authService.getMachineTokenForTjeneste(veilarbarenaAadTokenScope)
            }
        }
            .map { TokenResult.Success(it) }
            .getOrElse { TokenResult.Fail("Feilet å hente token for veilarbarena" , it)
        }
    }

    private fun buildRequest(url: String, payload: PersonRequest, token: String): Request {
        return Request.Builder()
            .url(url)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .post(JsonUtils.toJson(payload).toRequestBody(RestUtils.MEDIA_TYPE_JSON))
            .build()
    }

    private fun <T : Any> httpPost(url: String, payload: PersonRequest, clazz: Class<T>): RequestResult<T> {
        when (val tokenResult = getToken()) {
            is TokenResult.Fail -> return RequestResult.Fail(tokenResult.message, tokenResult.reason)
            is TokenResult.Success -> {
                val request = buildRequest(url, payload, tokenResult.token)
                client.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        return RequestResult.Success(Optional.empty())
                    }
                    /* Kun forventet å få 422 på registrer endepunkt, da ser body lik ut som i 200 respons */
                    if (response.code == 422) {
                        return Optional.of(
                            RestUtils.parseJsonResponseOrThrow(
                                response,
                                clazz
                            )
                        ).let { RequestResult.Success(it) }
                    }
                    RestUtils.throwIfNotSuccessful(response)
                    return Optional.of(
                        RestUtils.parseJsonResponseOrThrow(
                            response,
                            clazz
                        )
                    ).let { RequestResult.Success(it) }
                }
            }
        }

    }

    override fun hentOppfolgingsbruker(fnr: Fnr): Optional<VeilarbArenaOppfolgingsBruker> {
        val personRequest = PersonRequest(fnr)
        try {
            val response = httpPost(UrlUtils.joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/hent-oppfolgingsbruker"), personRequest, VeilarbArenaOppfolgingsBruker::class.java)
            return when (response) {
                is RequestResult.Success -> response.body
                is RequestResult.Fail -> Optional.empty()
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av oppfolgingsbruker fra veilarbarena", e)
            return Optional.empty()
        }
    }

    @SneakyThrows
    override fun getArenaOppfolgingsstatus(fnr: Fnr): Optional<VeilarbArenaOppfolgingsStatus> {
        val personRequest = PersonRequest(fnr)
        try {
            val response = httpPost(UrlUtils.joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/hent-oppfolgingsstatus"), personRequest, VeilarbArenaOppfolgingsStatus::class.java)
            return when (response) {
                is RequestResult.Success -> response.body
                is RequestResult.Fail -> Optional.empty()
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av oppfolgingsstatus fra veilarbarena", e)
            return Optional.empty()
        }
    }

    override fun getArenaYtelser(fnr: Fnr): Optional<YtelserDTO> {
        val personRequest = PersonRequest(fnr)
        try {
            val response = httpPost(UrlUtils.joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/arena/hent-ytelser"), personRequest, YtelserDTO::class.java)
            return when (response) {
                is RequestResult.Success -> response.body
                is RequestResult.Fail -> Optional.empty()
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av ytelser fra veilarbarena", e)
            return Optional.empty()
        }
    }

    override fun registrerIkkeArbeidsoker(fnr: Fnr): RegistrerIArenaResult {
        val personRequest = PersonRequest(fnr)

        try {
            val response = httpPost(UrlUtils.joinPaths(veilarbarenaUrl,
                "/veilarbarena/api/v2/arena/registrer-i-arena"),
                personRequest,
                RegistrerIkkeArbeidssokerDto::class.java)
            emptySuccessMeansFailure(response)
            return when (response) {
                is RequestResult.Success -> RegistrerIArenaSuccess(response.body.get())
                is RequestResult.Fail ->  RegistrerIArenaError("Noe gikk galt ved registrering av bruker i Arena", response.reason)
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved registrer bruker via veilarbarena", e)
            throw e
        }
    }

    private fun emptySuccessMeansFailure(response: RequestResult<RegistrerIkkeArbeidssokerDto>) {
        if (response is RequestResult.Success && response.body.isEmpty) {
            throw RuntimeException("Ugyldig url for registrering av bruker i Arena")
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(UrlUtils.joinPaths(veilarbarenaUrl, "/veilarbarena/internal/isAlive"), client)
    }
}