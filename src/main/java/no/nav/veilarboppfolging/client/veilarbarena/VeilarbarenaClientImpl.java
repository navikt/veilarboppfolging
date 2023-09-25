package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import okhttp3.*;

import java.util.Optional;
import java.util.function.Function;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class VeilarbarenaClientImpl implements VeilarbarenaClient {

    private static final DownstreamApi veilarbArenaApi = new DownstreamApi(EnvironmentUtils.requireClusterName(), "pto", "veilarbarena");

    private final String veilarbarenaUrl;


    private final Function<DownstreamApi, String> aadOboTokenProvider;
    private final Function<DownstreamApi, String> machineTokenProvider;
    private final AuthService authService;

    private final OkHttpClient client;

    public VeilarbarenaClientImpl(String veilarbarenaUrl, Function<DownstreamApi, String> machineTokenProvider, Function<DownstreamApi, String> aadOboTokenProvider, AuthService authService) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.machineTokenProvider = machineTokenProvider;
        this.client = RestClient.baseClient();
        this.aadOboTokenProvider = aadOboTokenProvider;
        this.authService = authService;
    }

    private String getToken() {
        if (authService.erInternBruker()) {
            return aadOboTokenProvider.apply(veilarbArenaApi);
        } else {
            return machineTokenProvider.apply(veilarbArenaApi);
        }
    }

    @Override
    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsbruker/" + fnr.get()))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                return Optional.empty();
            }

            RestUtils.throwIfNotSuccessful(response);
            return Optional.of(RestUtils.parseJsonResponseOrThrow(response, VeilarbArenaOppfolging.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SneakyThrows
    @Override
    public Optional<ArenaOppfolging> getArenaOppfolgingsstatus(Fnr fnr) {
        RequestBody fnrBody = new FormBody.Builder()
                .add("fnr", fnr.get())
                .build();
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsstatus/"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
                .post(fnrBody)
                .build();
        secureLog.info("veilarbarena getArenaOppfolgingsstatus: {}", fnrBody);
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                return Optional.empty();
            }

            RestUtils.throwIfNotSuccessful(response);
            return Optional.of(RestUtils.parseJsonResponseOrThrow(response, ArenaOppfolging.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/internal/isAlive"), client);
    }

}
