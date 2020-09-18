package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifClientImpl;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.test.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.junit.Assert.*;

public class DkifClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void skal_hente_kontaktinfo() {
        String kodeverkJson = TestUtils.readTestResourceFile("dkif-kontaktinfo.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        DkifClient dkifClient = new DkifClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(anyUrl())
                .withHeader("Nav-Personidenter", equalTo(TEST_FNR))
                .withHeader("Authorization", equalTo("Bearer TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(kodeverkJson))
        );

        SubjectHandler.withSubject(new Subject("test", IdentType.InternBruker, SsoToken.oidcToken("token", new HashMap<>())), () -> {
            DkifKontaktinfo kontaktinfo = dkifClient.hentKontaktInfo(TEST_FNR);
            assertEquals(kontaktinfo.getPersonident(), TEST_FNR);
            assertTrue(kontaktinfo.isKanVarsles());
            assertFalse(kontaktinfo.isReservert());
            assertEquals(kontaktinfo.getEpostadresse(), "noreply@nav.no");
            assertEquals(kontaktinfo.getMobiltelefonnummer(), "11111111");
        });
    }

}
