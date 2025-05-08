package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.*;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.AssertUtils.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    private final AuthService authServiceMock = mock(AuthService.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        when(authServiceMock.getMachineTokenForTjeneste(anyString())).thenReturn("token here");
        System.setProperty("NAIS_CLUSTER_NAME", "dev-fss");
    }

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        String apiScope = "api://local.pto.veilarbarena/.default";
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE_JSON.toString())
                        .withBody(toJson(arenaOppfolgingResponse())))
        );

        VeilarbArenaOppfolgingsStatus veilarbArenaOppfolgingsStatus = veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).orElseThrow();

        assertThat(veilarbArenaOppfolgingsStatus.getFormidlingsgruppe()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        assertThat(veilarbArenaOppfolgingsStatus.getOppfolgingsenhet()).isEqualTo(MOCK_ENHET_ID);
        assertThat(veilarbArenaOppfolgingsStatus.getRettighetsgruppe()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        assertThat(veilarbArenaOppfolgingsStatus.getServicegruppe()).isEqualTo(MOCK_SERVICEGRUPPE);
        assertThat(veilarbArenaOppfolgingsStatus.getKanEnkeltReaktiveres()).isEqualTo(MOCK_KAN_ENKELT_REAKTIVERES);
    }

    @Test
    public void skal_returnere_empty_om_person_ikke_funnet() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope,authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_man_ikke_har_tilgang() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(403)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_token_utveksling_feiler() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);
        when(authServiceMock.getMachineTokenForTjeneste(anyString())).thenThrow(new IllegalArgumentException("Lol"));

        var oppfolgingsstatus = veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);

        assertTrue(oppfolgingsstatus.isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_ugyldig_identifikator() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsstatus")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(400)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    private VeilarbArenaOppfolgingsStatus arenaOppfolgingResponse() {
        return new VeilarbArenaOppfolgingsStatus()
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

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE_JSON.toString())
                        .withBody(toJson(arenaOppfolgingsBrukerResponse())))
        );

        VeilarbArenaOppfolgingsBruker arenaOppfolgingsbruker = veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).orElseThrow();

        assertThat(arenaOppfolgingsbruker.getFodselsnr()).isEqualTo("1234");
        assertThat(arenaOppfolgingsbruker.getFormidlingsgruppekode()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        assertThat(arenaOppfolgingsbruker.getRettighetsgruppekode()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        assertThat(arenaOppfolgingsbruker.getNav_kontor()).isEqualTo(MOCK_ENHET_ID);
        assertThat(arenaOppfolgingsbruker.getHovedmaalkode()).isEqualTo(MOCK_HOVEDMAAL);
    }

    @Test
    public void skal_returnere_empty_om_person_ikke_funnet_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope,authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_man_ikke_har_tilgang_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(403)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_ugyldig_identifikator_oppfolgingsbruker() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/hent-oppfolgingsbruker")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(400)));

        assertTrue(veilarbarenaClient.hentOppfolgingsbruker(MOCK_FNR).isEmpty());
    }

    @Test
    public void registrer_ikke_arbeidssoker__skal_returnere_ok_om_alt_gaar_bra() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        RegistrerIkkeArbeidssokerDto registrerIkkeArbeidsokerRespons = new RegistrerIkkeArbeidssokerDto(
                "Ny bruker ble registrert ok som IARBS",
                ArenaRegistreringResultat.OK_REGISTRERT_I_ARENA);
        givenThat(post(urlEqualTo("/veilarbarena/api/v2/arena/registrer-i-arena")).withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(200).withBody(toJson(registrerIkkeArbeidsokerRespons))));

        RegistrerIArenaResult foreventetResponse = veilarbarenaClient.registrerIkkeArbeidsoker(MOCK_FNR);
        assertThat(foreventetResponse).isInstanceOf(RegistrerIArenaSuccess.class);
        var forventetDto = ((RegistrerIArenaSuccess) foreventetResponse).getArenaResultat();
        assertThat(forventetDto).isEqualTo(registrerIkkeArbeidsokerRespons);
    }

    @Test
    public void registrer_ikke_arbeidssoker_skal_returnere_dto__dersom_person_ikke_aktivert() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        var registrerIkkeArbeidsokerRespons = new RegistrerIkkeArbeidssokerDto(
                "Eksisterende bruker er ikke oppdatert da bruker kan reaktiveres forenklet som arbeidssøker", ArenaRegistreringResultat.KAN_REAKTIVERES_FORENKLET);
        givenThat(post(urlEqualTo("/veilarbarena/api/v2/arena/registrer-i-arena"))
                .withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(422).withBody(toJson(registrerIkkeArbeidsokerRespons)))
        );

        var response = veilarbarenaClient.registrerIkkeArbeidsoker(MOCK_FNR);
        assertThat(response).isInstanceOf(RegistrerIArenaSuccess.class);
        assertThat( ((RegistrerIArenaSuccess) response).getArenaResultat()).isEqualTo(registrerIkkeArbeidsokerRespons);
    }

    @Test
    public void registrer_ikke_arbeidssoker_skal_feile_dersom_vi_kaller_feil_url() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, apiScope, authServiceMock);

        givenThat(post(urlEqualTo("/veilarbarena/api/v2/arena/registrer-i-arena"))
                .withRequestBody(equalToJson("{\"fnr\":\""+MOCK_FNR+"\"}"))
                .willReturn(aResponse().withStatus(404))
        );

        assertThrows(RuntimeException.class, () ->veilarbarenaClient.registrerIkkeArbeidsoker(MOCK_FNR));

    }

    private VeilarbArenaOppfolgingsBruker arenaOppfolgingsBrukerResponse() {
        return new VeilarbArenaOppfolgingsBruker()
                .setFodselsnr("1234")
                .setFormidlingsgruppekode(MOCK_FORMIDLINGSGRUPPE)
                .setKvalifiseringsgruppekode(MOCK_KVALIFISERINGSGRUPPE)
                .setRettighetsgruppekode(MOCK_RETTIGHETSGRUPPE)
                .setNav_kontor(MOCK_ENHET_ID)
                .setHovedmaalkode(MOCK_HOVEDMAAL);
    }
}

