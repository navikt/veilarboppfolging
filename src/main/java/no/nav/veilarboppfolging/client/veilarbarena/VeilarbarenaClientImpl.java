package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Optional;
import java.util.function.Function;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class VeilarbarenaClientImpl implements VeilarbarenaClient {

    private final String veilarbarenaUrl;
    private final String veilarbarenaAadTokenScope;
    private final AuthService authService;
    private final OkHttpClient client;

    public VeilarbarenaClientImpl(String veilarbarenaUrl, String veilarbarenaAadTokenScope,  AuthService authService) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.veilarbarenaAadTokenScope = veilarbarenaAadTokenScope;
        this.client = RestClient.baseClient();
        this.authService = authService;
    }

    private String getToken() {
        if (authService.erInternBruker()) {
            return authService.getAadOboTokenForTjeneste(veilarbarenaAadTokenScope);
        } else {
            return authService.getMachineTokenForTjeneste(veilarbarenaAadTokenScope);
        }
    }

    @Override
    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr) {
    	PersonRequest personRequest = new PersonRequest(fnr);
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/hent-oppfolgingsbruker"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
				.post(RequestBody.create(JsonUtils.toJson(personRequest), MEDIA_TYPE_JSON))
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
		PersonRequest personRequest = new PersonRequest(fnr);
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/hent-oppfolgingsstatus"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
				.post(RequestBody.create(JsonUtils.toJson(personRequest), MEDIA_TYPE_JSON))
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
    public Optional<YtelserDTO> getArenaYtelser(Fnr fnr) {
        PersonRequest personRequest = new PersonRequest(fnr);
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/veilarbarena/api/v2/hent-ytelser"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + getToken())
                .post(RequestBody.create(JsonUtils.toJson(personRequest), MEDIA_TYPE_JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                return Optional.empty();
            }

            RestUtils.throwIfNotSuccessful(response);
            return Optional.of(RestUtils.parseJsonResponseOrThrow(response, YtelserDTO.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/veilarbarena/internal/isAlive"), client);
    }

}
