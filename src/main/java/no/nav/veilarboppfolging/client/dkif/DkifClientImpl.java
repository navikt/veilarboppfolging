package no.nav.veilarboppfolging.client.dkif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarboppfolging.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarboppfolging.utils.RestClientUtils.authHeaderMedInnloggetBruker;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class DkifClientImpl implements DkifClient {

    private final String dkifUrl;

    private final OkHttpClient client;

    public DkifClientImpl(String dkifUrl) {
        this.dkifUrl = dkifUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.DKIF_KONTAKTINFO_CACHE_NAME)
    @SneakyThrows
    @Override
    public DkifKontaktinfo hentKontaktInfo(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(dkifUrl, "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, authHeaderMedInnloggetBruker())
                .header("Nav-Personidenter", fnr)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            Optional<String> json = RestUtils.getBodyStr(response);

            if (!json.isPresent()) {
                throw new IllegalStateException("Dkif body is missing");
            }

            ObjectMapper mapper = JsonUtils.getMapper();
            JsonNode node = mapper.readTree(json.get());

            return mapper.treeToValue(node.get("kontaktinfo").get(fnr), DkifKontaktinfo.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(dkifUrl, "/internal/isAlive"), client);
    }

}
