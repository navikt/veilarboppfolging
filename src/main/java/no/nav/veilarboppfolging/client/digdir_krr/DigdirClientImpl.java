package no.nav.veilarboppfolging.client.digdir_krr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class DigdirClientImpl implements DigdirClient {

    public static final String CALL_ID = "callId";
    public static final String NAV_CALL_ID = "Nav-Call-Id";
    private String getCallId() {
        return isBlank(MDC.get(CALL_ID)) ? UUID.randomUUID().toString() : MDC.get(CALL_ID);
    }

    private final String digdirUrl;

    private final SystemUserTokenProvider systemUserTokenProvider;

    private final AuthContextHolder authContextHolder;

    private final OkHttpClient client;

    public DigdirClientImpl(String digdirUrl, SystemUserTokenProvider systemUserTokenProvider, AuthContextHolder authContextHolder) {
        this.digdirUrl = digdirUrl;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.authContextHolder = authContextHolder;
        this.client = RestClient
                .baseClientBuilder()
                .callTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Cacheable(CacheConfig.DIGDIR_KONTAKTINFO_CACHE_NAME)
    @SneakyThrows
    @Override
    public Optional<DigdirKontaktinfo> hentKontaktInfo(Fnr fnr) {
        String authorization = authContextHolder.getIdTokenString().isPresent() ? authContextHolder.getIdTokenString().get() : "";
        String issuer = authContextHolder.getIdTokenClaims().map(JWTClaimsSet::getIssuer).orElse("");
        Request request = new Request.Builder()
                .url(joinPaths(digdirUrl, "/api/v1/person?inkluderSikkerDigitalPost=false"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + authorization)
                .header(NAV_CALL_ID, getCallId())
                .header("Nav-personident", fnr.get())
                .build();

        try (Response response = client.newCall(request).execute()) {

            log.info("svar fra digdir: message = {}, challanges = {}, AuthContextHolder = {}, callId = {}, issuer = {}", response.message(), response.challenges(), authContextHolder.getIdTokenString().get(), getCallId(), issuer);
            RestUtils.throwIfNotSuccessful(response);
            String json = RestUtils.getBodyStr(response)
                    .orElseThrow(() -> new IllegalStateException("Response body from Digdir_KRR is missing"));

            ObjectMapper mapper = JsonUtils.getMapper();

            JsonNode node = mapper.readTree(json);
            JsonNode kontaktinfoNode = ofNullable(node.get("kontaktinfo"))
                    .map(n -> n.get(fnr.get()))
                    .orElse(null);

            if (kontaktinfoNode == null) {
                return empty();
            }

            return Optional.of(mapper.treeToValue(kontaktinfoNode, DigdirKontaktinfo.class));
        } catch (Exception e) {
            log.error("Feil under henting av data fra Digdir_KRR", e);
            return empty();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(digdirUrl, "/api/ping"), client);
    }
}
