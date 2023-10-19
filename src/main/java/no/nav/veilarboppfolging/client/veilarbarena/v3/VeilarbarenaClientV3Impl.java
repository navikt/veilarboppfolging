package no.nav.veilarboppfolging.client.veilarbarena.v3;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.domain.PersonRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Optional;
import java.util.function.Function;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class VeilarbarenaClientV3Impl implements VeilarbarenaClientV3 {

    private static final DownstreamApi veilarbArenaApi = new DownstreamApi(EnvironmentUtils.requireClusterName(), "pto", "veilarbarena");

    private final String veilarbarenaUrl;


    private final Function<DownstreamApi, String> aadOboTokenProvider;
    private final Function<DownstreamApi, String> machineTokenProvider;
    private final AuthService authService;

    private final OkHttpClient client;

    public VeilarbarenaClientV3Impl(String veilarbarenaUrl, Function<DownstreamApi, String> machineTokenProvider, Function<DownstreamApi, String> aadOboTokenProvider, AuthService authService) {
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
    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbrukerV3(PersonRequest personRequest) {
        secureLog.info("v3 Arena hentOppfolgingsbruker postmapping innsendt ident: {}", personRequest.getFnr());
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/v2/oppfolgingsbruker/"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
                .post(RequestBody.create(personRequest.getFnr().toString(), MEDIA_TYPE_JSON))
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
    public Optional<ArenaOppfolging> getArenaOppfolgingsstatusV3(PersonRequest personRequest) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/v2/oppfolgingsstatus/"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
                .post(RequestBody.create(personRequest.getFnr().toString(), MEDIA_TYPE_JSON))
                .build();

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
