package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.LogRequestInterceptor;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.config.CacheConfig;
import no.nav.veilarboppfolging.service.AuthService;
import okhttp3.*;
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

	class KrrPersonerDto {
		KrrPersonerDto(String fnr) {
			this.personidenter = new String[]{fnr};
		}
		String[] personidenter;
	}

	@Cacheable(CacheConfig.DIGDIR_KONTAKTINFO_CACHE_NAME)
	@SneakyThrows
	@Override
	public Optional<KRRData> hentKontaktInfo(Fnr fnr) {
		var json = JsonUtils.toJson(new KrrPersonerDto(fnr.get()));
		var body = RequestBody.create(json, MediaType.parse("application/json"));
		Request request = new Request.Builder()
				.post(body)
				.url(joinPaths(digdirUrl, "/rest/v1/personer"))
				.header(ACCEPT, APPLICATION_JSON_VALUE)
				.header(AUTHORIZATION, "Bearer " + getToken())
				.build();

		try (Response response = client.newCall(request).execute()) {
			RestUtils.throwIfNotSuccessful(response);
			return RestUtils.parseJsonResponse(response, KrrPersonerResponseDto.class)
					.flatMap(KrrPersonerResponseDto::assertSinglePersonToKrrData);
		} catch (Exception e) {
			log.error("Feil under henting av data fra Digdir_KRR", e);
			return empty();
		}
	}

	private String getToken() {
		if (authService.erInternBruker()) {
			return userTokenProvider.get();
		}
		return systemUserTokenProvider.get();
	}
}
