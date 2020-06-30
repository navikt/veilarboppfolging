package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarboppfolging.domain.ArenaOppfolging;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class VeilarbarenaClientImpl implements VeilarbarenaClient {

    private final String veilarbarenaUrl;

    private final OkHttpClient client;

    public VeilarbarenaClientImpl(String veilarbarenaUrl) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.client = RestClient.baseClient();
    }

    @Override
    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsbruker/" + fnr))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "System user token")
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return Optional.of(RestUtils.parseJsonResponseOrThrow(response, VeilarbArenaOppfolging.class));
        } catch (Exception e) {
            log.warn("Kall til veilarbarena feilet", e);
            return Optional.empty();
        }
    }

    @SneakyThrows
    @Override
    public ArenaOppfolging getArenaOppfolgingsstatus(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsstatus/" + fnr))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "System user token")
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, ArenaOppfolging.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/internal/isAlive"), client);
    }

}
