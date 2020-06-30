package no.nav.veilarboppfolging.services;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.veilarboppfolging.config.ApplicationConfig.VEILARBARENAAPI_URL_PROPERTY;
import static no.nav.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static no.nav.json.JsonUtils.toJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArenaOppfolgingServiceTest {

    private static final String MOCK_FNR = "1234";
    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_FORMIDLINGSGRUPPE = "ARBS";
    private static final String MOCK_SERVICEGRUPPE = "servicegruppe";
    private static final boolean MOCK_KAN_ENKELT_REAKTIVERES = true;
    private static final String MOCK_RETTIGHETSGRUPPE = "rettighetsgruppe";

    private ArenaOppfolgingService arenaOppfolgingService;

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(VEILARBARENAAPI_URL_PROPERTY, "http://localhost:" + wireMockRule.port());
        arenaOppfolgingService = new ArenaOppfolgingService(oppfoelgingPortType, RestUtils.createClient());
    }

    @Test
    public void hentOppfoelgingskontraktListeReturnererEnRespons() throws Exception {
        final XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(LocalDate.now().minusMonths(2));
        final XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(LocalDate.now().plusMonths(1));
        HentOppfoelgingskontraktListeResponse withOppfoelgingskontraktListe = new HentOppfoelgingskontraktListeResponse();
        Oppfoelgingskontrakt oppfoelgingskontrakt = new Oppfoelgingskontrakt();
        oppfoelgingskontrakt.setGjelderBruker(new Bruker());
        withOppfoelgingskontraktListe.getOppfoelgingskontraktListe().add(oppfoelgingskontrakt);
        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(HentOppfoelgingskontraktListeRequest.class))).thenReturn(withOppfoelgingskontraktListe);

        final HentOppfoelgingskontraktListeResponse response = arenaOppfolgingService.hentOppfolgingskontraktListe(fom, tom, "fnr");

        assertThat(response.getOppfoelgingskontraktListe().isEmpty(), is(false));
    }

    @Test
    public void skalMappeTilOppfolgingsstatusV2() {
        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(toJson(arenaOppfolgingResponse())))
        );

        ArenaOppfolging arenaOppfolging = arenaOppfolgingService.hentArenaOppfolging(MOCK_FNR);
        Assertions.assertThat(arenaOppfolging.getFormidlingsgruppe()).isEqualTo(MOCK_FORMIDLINGSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getOppfolgingsenhet()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(arenaOppfolging.getRettighetsgruppe()).isEqualTo(MOCK_RETTIGHETSGRUPPE);
        Assertions.assertThat(arenaOppfolging.getServicegruppe()).isEqualTo(MOCK_SERVICEGRUPPE);
        Assertions.assertThat(arenaOppfolging.getKanEnkeltReaktiveres()).isEqualTo(MOCK_KAN_ENKELT_REAKTIVERES);
    }

    @Test(expected = NotFoundException.class)
    public void skalKasteNotFoundOmPersonIkkeFunnet() {
        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(404)));

        arenaOppfolgingService.hentArenaOppfolging(MOCK_FNR);
    }

    @Test(expected = ForbiddenException.class)
    public void skalKasteForbiddenOmManIkkeHarTilgang() {
        givenThat(get(urlEqualTo("/oppfolgingsstatus/" +MOCK_FNR))
                .willReturn(aResponse().withStatus(403)));

        arenaOppfolgingService.hentArenaOppfolging(MOCK_FNR);
    }

    @Test(expected = BadRequestException.class)
    public void skalKasteBadRequestOmUgyldigIdentifikator() {
        givenThat(get(urlEqualTo("/oppfolgingsstatus/" + MOCK_FNR))
                .willReturn(aResponse().withStatus(400)));

        arenaOppfolgingService.hentArenaOppfolging(MOCK_FNR);
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
