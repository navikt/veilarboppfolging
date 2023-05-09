package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;

import no.nav.veilarboppfolging.test.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.readOnlyHttpHeaders;

public class DigdirClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "dev-fss");
    }

	@Test
    public void hentKontaktInfo__skal_hente_kontaktinfo() {
        String digdirJson = TestUtils.readTestResourceFile("client/digdir/kontaktinfo.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        DigdirClientImpl digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(anyUrl())
				.withHeader("Authorization", equalTo("Bearer TOKEN"))
				.withHeader(ACCEPT, equalTo("application/json"))
				.withHeader("Nav-Personident", equalTo(TEST_FNR.get()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(digdirJson))
        );

        DigdirKontaktinfo kontaktinfo = digdirClient.hentKontaktInfo(TEST_FNR).orElseThrow();
        assertEquals(kontaktinfo.getPersonident(), TEST_FNR.get());
        assertFalse(kontaktinfo.isKanVarsles());
        assertTrue(kontaktinfo.isReservert());
        assertEquals("noreply@nav.no", kontaktinfo.getEpostadresse());
        assertEquals("11111111", kontaktinfo.getMobiltelefonnummer());
    }

    @Test
    public void hentKontaktInfo__skal_returnere_empty_hvis_ingen_kontaktinfo() {
        String kodeverkJson = TestUtils.readTestResourceFile("client/digdir/no-kontaktinfo.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        DigdirClientImpl digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(anyUrl())
				.withHeader("Authorization", equalTo("Bearer TOKEN"))
				.withHeader(ACCEPT, equalTo("application/json"))
				.withHeader("Nav-Personident", equalTo(TEST_FNR.get()))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody(kodeverkJson))
        );

        assertTrue(digdirClient.hentKontaktInfo(TEST_FNR).isEmpty());
    }


}
