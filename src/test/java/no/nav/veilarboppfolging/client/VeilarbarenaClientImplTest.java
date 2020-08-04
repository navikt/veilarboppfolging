package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.json.JsonUtils.toJson;

public class VeilarbarenaClientImplTest {

    private static final String MOCK_FNR = "1234";
    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_FORMIDLINGSGRUPPE = "ARBS";
    private static final String MOCK_SERVICEGRUPPE = "servicegruppe";
    private static final boolean MOCK_KAN_ENKELT_REAKTIVERES = true;
    private static final String MOCK_RETTIGHETSGRUPPE = "rettighetsgruppe";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(toJson(arenaOppfolgingResponse())))
        );

        ArenaOppfolging arenaOppfolging = veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
        Assertions.assertThat(arenaOppfolging.getFormidlingsgruppe()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getOppfolgingsenhet()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(arenaOppfolging.getRettighetsgruppe()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getServicegruppe()).isEqualTo(MOCK_SERVICEGRUPPE);
        Assertions.assertThat(arenaOppfolging.getKanEnkeltReaktiveres()).isEqualTo(MOCK_KAN_ENKELT_REAKTIVERES);
    }

    @Test(expected = RuntimeException.class)
    public void skalKasteNotFoundOmPersonIkkeFunnet() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(404)));

        veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
    }

    @Test(expected = RuntimeException.class)
    public void skalKasteForbiddenOmManIkkeHarTilgang() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" +MOCK_FNR))
                .willReturn(aResponse().withStatus(403)));

        veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
    }

    @Test(expected = RuntimeException.class)
    public void skalKasteBadRequestOmUgyldigIdentifikator() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(400)));

        veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
    }

    private ArenaOppfolging arenaOppfolgingResponse() {
        return new ArenaOppfolging()
                .setFormidlingsgruppe(MOCK_FORMIDLINGSGRUPPE)
                .setServicegruppe(MOCK_SERVICEGRUPPE)
                .setOppfolgingsenhet(MOCK_ENHET_ID)
                .setKanEnkeltReaktiveres(MOCK_KAN_ENKELT_REAKTIVERES)
                .setRettighetsgruppe(MOCK_RETTIGHETSGRUPPE);
    }
}
