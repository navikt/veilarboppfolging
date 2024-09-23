package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.AssertUtils.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class VeilarbarenaClientImplTest {

    private static final Fnr MOCK_FNR = Fnr.of("1234");
    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_FORMIDLINGSGRUPPE = "ARBS";
    private static final String MOCK_SERVICEGRUPPE = "servicegruppe";
    private static final boolean MOCK_KAN_ENKELT_REAKTIVERES = true;
    private static final String MOCK_RETTIGHETSGRUPPE = "rettighetsgruppe";
    private static final String MOCK_KVALIFISERINGSGRUPPE = "kvalifiseringsgruppe";
    String apiScope = "api://local.pto.veilarbarena/.default";

    private static final String MOCK_HOVEDMAAL = "beholde arbeid";

    private AuthService authServiceMock = mock(AuthService.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "dev-fss");
    }

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        String apiScope = "api://local.pto.veilarbarena/.default";
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE_JSON.toString())
                        .withBody(toJson(arenaOppfolgingResponse())))
        );

        ArenaOppfolging arenaOppfolging = veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).orElseThrow();

        Assertions.assertThat(arenaOppfolging.getFormidlingsgruppe()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getOppfolgingsenhet()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(arenaOppfolging.getRettighetsgruppe()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getServicegruppe()).isEqualTo(MOCK_SERVICEGRUPPE);
        Assertions.assertThat(arenaOppfolging.getKanEnkeltReaktiveres()).isEqualTo(MOCK_KAN_ENKELT_REAKTIVERES);
    }

    @Test
    public void skal_returnere_empty_om_person_ikke_funnet() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope,authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_man_ikke_har_tilgang() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(403)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_ugyldig_identifikator() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(400)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    private ArenaOppfolging arenaOppfolgingResponse() {
        return new ArenaOppfolging()
                .setFormidlingsgruppe(MOCK_FORMIDLINGSGRUPPE)
                .setServicegruppe(MOCK_SERVICEGRUPPE)
                .setOppfolgingsenhet(MOCK_ENHET_ID)
                .setKanEnkeltReaktiveres(MOCK_KAN_ENKELT_REAKTIVERES)
                .setRettighetsgruppe(MOCK_RETTIGHETSGRUPPE);
    }
    @Test
    public void skalMappeTilOppfolgingsbrukerV2() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE_JSON.toString())
                        .withBody(toJson(arenaOppfolgingsBrukerResponse())))
        );

        VeilarbArenaOppfolging arenaOppfolgingsbruker = veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).orElseThrow();

        Assertions.assertThat(arenaOppfolgingsbruker.getFodselsnr()).isEqualTo("1234");
        Assertions.assertThat(arenaOppfolgingsbruker.getFormidlingsgruppekode()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        Assertions.assertThat(arenaOppfolgingsbruker.getRettighetsgruppekode()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        Assertions.assertThat(arenaOppfolgingsbruker.getNav_kontor()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(arenaOppfolgingsbruker.getHovedmaalkode()).isEqualTo(MOCK_HOVEDMAAL);
    }

    @Test
    public void skal_returnere_empty_om_person_ikke_funnet_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope,authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_man_ikke_har_tilgang_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(403)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_ugyldig_identifikator_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(400)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    private VeilarbArenaOppfolging arenaOppfolgingsBrukerResponse() {
        return new VeilarbArenaOppfolging()
                .setFodselsnr("1234")
                .setFormidlingsgruppekode(MOCK_FORMIDLINGSGRUPPE)
                .setKvalifiseringsgruppekode(MOCK_KVALIFISERINGSGRUPPE)
                .setRettighetsgruppekode(MOCK_RETTIGHETSGRUPPE)
                .setNav_kontor(MOCK_ENHET_ID)
                .setHovedmaalkode(MOCK_HOVEDMAAL);
    }
}

