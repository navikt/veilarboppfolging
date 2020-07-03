package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.json.JsonUtils.toJson;

@RunWith(MockitoJUnitRunner.class)
public class VeilarbarenaClientImplTest {

    private static final String MOCK_FNR = "1234";
    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_FORMIDLINGSGRUPPE = "ARBS";
    private static final String MOCK_SERVICEGRUPPE = "servicegruppe";
    private static final boolean MOCK_KAN_ENKELT_REAKTIVERES = true;
    private static final String MOCK_RETTIGHETSGRUPPE = "rettighetsgruppe";

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

//    @Before
//    public void setup() {
//        systemPropertiesRule.setProperty(VEILARBARENAAPI_URL_PROPERTY, "http://localhost:" + wireMockRule.port());
//        arenaOppfolgingService = new ArenaOppfolgingService(oppfoelgingPortType, RestUtils.createClient());
//    }

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl("", () -> "TOKEN");

        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
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

    @Test(expected = NotFoundException.class)
    public void skalKasteNotFoundOmPersonIkkeFunnet() {
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl("", () -> "TOKEN");

        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(404)));

        veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
    }

    @Test(expected = ForbiddenException.class)
    public void skalKasteForbiddenOmManIkkeHarTilgang() {
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl("", () -> "TOKEN");

        givenThat(get(urlEqualTo("/oppfolgingsstatus/" +MOCK_FNR))
                .willReturn(aResponse().withStatus(403)));

        veilarbarenaClient.getArenaOppfolgingsstatus(MOCK_FNR);
    }

    @Test(expected = BadRequestException.class)
    public void skalKasteBadRequestOmUgyldigIdentifikator() {
        VeilarbarenaClientImpl veilarbarenaClient = new VeilarbarenaClientImpl("", () -> "TOKEN");

        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
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
