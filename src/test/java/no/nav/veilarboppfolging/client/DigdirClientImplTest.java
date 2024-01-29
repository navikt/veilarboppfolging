package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;

import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.test.TestUtils;

import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;

public class DigdirClientImplTest {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(0);

	private final AuthService authService = mock(AuthService.class);

	@Test
	public void hentKontaktInfo__skal_hente_kontaktinfo() {
		String digdirJson = TestUtils.readTestResourceFile("client/digdir/kontaktinfo.json");
		String apiUrl = "http://localhost:" + wireMockRule.port();
		when(authService.erInternBruker()).thenReturn(true);
		DigdirClient digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN", () -> "TOKEN", authService);

		givenThat(get(anyUrl())
				.withHeader("Authorization", equalTo("Bearer TOKEN"))
				.withHeader(ACCEPT, equalTo("application/json"))
				.withHeader("Nav-Personident", equalTo(TEST_FNR.get()))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(digdirJson))
		);

		KRRData kontaktinfo = digdirClient.hentKontaktInfo(TEST_FNR).orElseThrow();
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
		when(authService.erInternBruker()).thenReturn(true);
		DigdirClient digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN", () -> "TOKEN", authService);

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
