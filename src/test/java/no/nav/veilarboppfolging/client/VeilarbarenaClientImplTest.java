package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.utils.DownstreamApi;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.AssertUtils.assertTrue;

public class VeilarbarenaClientImplTest {

    private static final Fnr MOCK_FNR = Fnr.of("1234");
    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_FORMIDLINGSGRUPPE = "ARBS";
    private static final String MOCK_SERVICEGRUPPE = "servicegruppe";
    private static final boolean MOCK_KAN_ENKELT_REAKTIVERES = true;
    private static final String MOCK_RETTIGHETSGRUPPE = "rettighetsgruppe";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "dev-fss");
    }

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN", (DownstreamApi v) -> Optional.of("TOKEN"));

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
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
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN", (DownstreamApi v) -> Optional.of("TOKEN"));

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_man_ikke_har_tilgang() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN", (DownstreamApi v) -> Optional.of("TOKEN"));

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(403)));

        assertTrue(veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR).isEmpty());
    }

    @Test
    public void skal_returnere_empty_om_ugyldig_identifikator() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl(apiUrl, () -> "TOKEN", (DownstreamApi v) -> Optional.of("TOKEN"));

        givenThat(get(urlEqualTo("/api/oppfolgingsstatus/" + MOCK_FNR))
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
}
