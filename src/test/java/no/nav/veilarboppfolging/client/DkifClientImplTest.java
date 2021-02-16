package no.nav.veilarboppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifClientImpl;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.test.TestUtils;
import org.junit.Rule;
import org.junit.Test;

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
        AuthContext context = AuthTestUtils.createAuthContext(UserRole.INTERN, "test");
        AuthContextHolderThreadLocal.instance().withContext(context, () -> {
            DkifKontaktinfo kontaktinfo = dkifClient.hentKontaktInfo(TEST_FNR);
            assertEquals(kontaktinfo.getPersonident(), TEST_FNR);
            assertTrue(kontaktinfo.isKanVarsles());
            assertFalse(kontaktinfo.isReservert());
            assertEquals(kontaktinfo.getEpostadresse(), "noreply@nav.no");
            assertEquals(kontaktinfo.getMobiltelefonnummer(), "11111111");
        });
    }

}
