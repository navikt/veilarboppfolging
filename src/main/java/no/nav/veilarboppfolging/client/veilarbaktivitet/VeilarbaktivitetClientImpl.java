package no.nav.veilarboppfolging.client.veilarbaktivitet;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class VeilarbaktivitetClientImpl implements VeilarbaktivitetClient {

    private final String veilarbaktivitetUrl;

    private final Supplier<String> userTokenProvider;

    private final OkHttpClient client;

    public VeilarbaktivitetClientImpl(String veilarbaktivitetUrl, Supplier<String> userTokenProvider) {
        this.veilarbaktivitetUrl = veilarbaktivitetUrl;
        this.userTokenProvider = userTokenProvider;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    @Override
    public List<ArenaAktivitetDTO> hentArenaAktiviteter(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbaktivitetUrl, "/api/aktivitet/arena?fnr=" + fnr))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + userTokenProvider.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, ArenaAktivitetDTO.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbaktivitetUrl, "/internal/isAlive"), client);
    }
}
