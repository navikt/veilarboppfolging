package no.nav.veilarboppfolging.client.digdir_krr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.Fnr;

import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.config.CacheConfig;


import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class DigdirClientImpl implements DigdirClient {
    public static final String CALL_ID = "callId";
    public static final String NAV_CALL_ID = "Nav-Call-Id";
	private static final DownstreamApi digDirApi = new DownstreamApi("dev-gcp", "team-rocket", "digdir-krr-proxy");
    private String getCallId() {
        return isBlank(MDC.get(CALL_ID)) ? UUID.randomUUID().toString() : MDC.get(CALL_ID);
    }

    private final String digdirUrl;

	private final Function<DownstreamApi, String> aadOboTokenProvider;

    private final OkHttpClient client;

    public DigdirClientImpl(String digdirUrl, Function<DownstreamApi, String> aadOboTokenProvider) {
        this.digdirUrl = digdirUrl;
        this.aadOboTokenProvider = aadOboTokenProvider;
        this.client = RestClient
                .baseClientBuilder()
                .callTimeout(Duration.ofSeconds(3))
                .build();
    }

	private String getToken() {
		try {
			return aadOboTokenProvider.apply(digDirApi);
		} catch (Exception e) {
			log.info("gikk ikke å hente token. message = {}, error ={}", e.getMessage(), e.getStackTrace());
			return "";
		}
	}

    @Cacheable(CacheConfig.DIGDIR_KONTAKTINFO_CACHE_NAME)
    @SneakyThrows
    @Override
    public Optional<DigdirKontaktinfo> hentKontaktInfo(Fnr fnr) {
		PersonIdenter personIdenter = new PersonIdenter().setPersonidenter(List.of(fnr.get()));

        Request request = new Request.Builder()
                .url(joinPaths(digdirUrl, "/api/v1/person?inkluderSikkerDigitalPost=false"))
                .header(ACCEPT, MEDIA_TYPE_JSON.toString())
                .header(AUTHORIZATION, "Bearer " + getToken())
                .header(NAV_CALL_ID, getCallId())
				.post(RestUtils.toJsonRequestBody(personIdenter))
                .build();

        try (Response response = client.newCall(request).execute()) {

            log.info("svar fra digdir: challenges = {}, WWW-Authenticate = {}", response.challenges(), response.headers().get("WWW-Authenticate"));
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
