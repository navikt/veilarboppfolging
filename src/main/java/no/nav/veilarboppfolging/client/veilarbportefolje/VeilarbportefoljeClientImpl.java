package no.nav.veilarboppfolging.client.veilarbportefolje;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.String.format;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class VeilarbportefoljeClientImpl implements VeilarbportefoljeClient {

    private final String veilarbportefoljeUrl;

    private final SystemUserTokenProvider openAmTokenProvider;

    private final OkHttpClient client;

    public VeilarbportefoljeClientImpl(String veilarbportefoljeUrl, SystemUserTokenProvider openAmTokenProvider) {
        this.veilarbportefoljeUrl = veilarbportefoljeUrl;
        this.openAmTokenProvider = openAmTokenProvider;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    @Override
    public OppfolgingEnhetPageDTO hentEnhetPage(int pageNumber, int pageSize) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbportefoljeUrl, format("/oppfolgingenhet?page_number=%d&page_size=%d", pageNumber, pageSize)))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Bearer " + openAmTokenProvider.getSystemUserToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, OppfolgingEnhetPageDTO.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbportefoljeUrl, "/internal/isAlive"), client);
    }

}
