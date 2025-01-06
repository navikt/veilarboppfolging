package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.LogRequestInterceptor;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.config.CacheConfig;
import no.nav.veilarboppfolging.service.AuthService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Optional.empty;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class DigdirClientImpl implements DigdirClient {

	private final String digdirUrl;

	private final Supplier<String> systemUserTokenProvider;

	private final Supplier<String> userTokenProvider;

	private final AuthService authService;

	private final OkHttpClient client;

	public DigdirClientImpl(String digdirUrl, Supplier<String> systemUserTokenProvider, Supplier<String> userTokenProvider, AuthService authService) {
		this.digdirUrl = digdirUrl;
		this.systemUserTokenProvider = systemUserTokenProvider;
		this.userTokenProvider = userTokenProvider;
		this.authService = authService;
		this.client = new OkHttpClient.Builder()
				.addInterceptor(new LogRequestInterceptor())
				.connectTimeout(1, TimeUnit.SECONDS)
				.readTimeout(5, TimeUnit.SECONDS)
				.writeTimeout(5, TimeUnit.SECONDS)
				.followRedirects(false)
				.build();
	}

	@Cacheable(CacheConfig.DIGDIR_KONTAKTINFO_CACHE_NAME)
	@SneakyThrows
	@Override
	public Optional<KRRData> hentKontaktInfo(Fnr fnr) {
		Request request = new Request.Builder()
				.url(joinPaths(digdirUrl, "/rest/v1/person?inkluderSikkerDigitalPost=false"))
				.header(ACCEPT, APPLICATION_JSON_VALUE)
				.header(AUTHORIZATION, "Bearer " + getToken())
				.header("Nav-Personident", fnr.get())
				.build();

		try (Response response = client.newCall(request).execute()) {
			RestUtils.throwIfNotSuccessful(response);
			return RestUtils.parseJsonResponse(response, DigdirKontaktinfo.class)
					.map(DigdirKontaktinfo::toKrrData);
		} catch (Exception e) {
			log.error("Feil under henting av data fra Digdir_KRR", e);
			return empty();
		}
	}

	@Override
	public HealthCheckResult checkHealth() {
		return HealthCheckUtils.pingUrl(joinPaths(digdirUrl, "/api/ping"), client);
	}

	private String getToken() {
		if (authService.erInternBruker()) {
			return userTokenProvider.get();
		}
		return systemUserTokenProvider.get();
	}
}
