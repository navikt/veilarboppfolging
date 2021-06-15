package no.nav.veilarboppfolging.client.dkif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class DkifClientImpl implements DkifClient {

    private final String dkifUrl;

    private final SystemUserTokenProvider systemUserTokenProvider;

    private final OkHttpClient client;

    public DkifClientImpl(String dkifUrl, SystemUserTokenProvider systemUserTokenProvider) {
        this.dkifUrl = dkifUrl;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.DKIF_KONTAKTINFO_CACHE_NAME)
    @SneakyThrows
    @Override
    public DkifKontaktinfo hentKontaktInfo(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(dkifUrl, "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .header("Nav-Personidenter", fnr.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            Optional<String> json = RestUtils.getBodyStr(response);

            if (json.isEmpty()) {
                log.warn("DKIF body is missing");
                return fallbackKontaktinfo(fnr);
            }

            ObjectMapper mapper = JsonUtils.getMapper();

            JsonNode node = mapper.readTree(json.get());
            JsonNode kontaktinfoNode = ofNullable(node.get("kontaktinfo"))
                    .map(n -> n.get(fnr.get()))
                    .orElse(null);

            if (kontaktinfoNode == null) {
                log.warn("Mangler kontaktinfo fra DKIF");
                return fallbackKontaktinfo(fnr);
            }

            return mapper.treeToValue(kontaktinfoNode, DkifKontaktinfo.class);
        } catch (Exception e) {
            log.error("Feil under henting av data fra DKIF", e);
            return fallbackKontaktinfo(fnr);
        }
    }

    private DkifKontaktinfo fallbackKontaktinfo(Fnr fnr) {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo();
        kontaktinfo.setPersonident(fnr.get());
        kontaktinfo.setKanVarsles(true);
        kontaktinfo.setReservert(false);
        return kontaktinfo;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(dkifUrl, "/api/ping"), client);
    }

}
