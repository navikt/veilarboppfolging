package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;
import no.nav.veilarboppfolging.test.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DigdirClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

	private SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
    private AuthContextHolder authContextHolder = mock(AuthContextHolder.class);
	@Test
    public void hentKontaktInfo__skal_hente_kontaktinfo() {
        String kodeverkJson = TestUtils.readTestResourceFile("client/digdir/kontaktinfo.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of("TOKEN"));
        DigdirClient digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN", authContextHolder);

        givenThat(get(anyUrl())
				.withHeader("Nav-personident", equalTo(TEST_FNR.get()))
				.withHeader("Authorization", equalTo("Bearer TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(kodeverkJson))
        );

        DigdirKontaktinfo kontaktinfo = digdirClient.hentKontaktInfo(TEST_FNR).orElseThrow();
        assertEquals(kontaktinfo.getPersonident(), TEST_FNR.get());
        assertTrue(kontaktinfo.isKanVarsles());
        assertFalse(kontaktinfo.isReservert());
        assertEquals("noreply@nav.no", kontaktinfo.getEpostadresse());
        assertEquals("11111111", kontaktinfo.getMobiltelefonnummer());
    }

    @Test
    public void hentKontaktInfo__skal_returnere_empty_hvis_ingen_kontaktinfo() {
        String kodeverkJson = TestUtils.readTestResourceFile("client/digdir/no-kontaktinfo.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of("TOKEN"));
        DigdirClient digdirClient = new DigdirClientImpl(apiUrl, () -> "TOKEN", authContextHolder);

        givenThat(get(anyUrl())
                .withHeader("Nav-Personident", equalTo(TEST_FNR.get()))
				.withHeader("Authorization", equalTo("Bearer TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(kodeverkJson))
        );

        assertTrue(digdirClient.hentKontaktInfo(TEST_FNR).isEmpty());
    }


}
