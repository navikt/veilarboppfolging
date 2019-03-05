package no.nav.fo.veilarboppfolging.rest;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.ConnectException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.fo.veilarboppfolging.rest.VeilArbAbacService.VEILARBABAC_HOSTNAME_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VeilArbAbacServiceTest {

    private static final String VEILEDER_ID = "veilederId";
    private static final String FNR = "fnr";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    private SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
    private VeilArbAbacService veilArbAbacService;

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(VEILARBABAC_HOSTNAME_PROPERTY, "http://localhost:" + wireMockRule.port());
        veilArbAbacService = new VeilArbAbacService(systemUserTokenProvider);
        when(systemUserTokenProvider.getToken()).thenReturn("system-token");
    }

    @Test
    public void ping() {
        givenThat(get(urlEqualTo("/ping"))
                .willReturn(aResponse().withStatus(200))
        );
        veilArbAbacService.helsesjekk();

        givenThat(get(urlEqualTo("/ping"))
                .willReturn(aResponse().withStatus(500))
        );
        assertThatThrownBy(() -> veilArbAbacService.helsesjekk()).isInstanceOf(IllegalStateException.class);

        wireMockRule.stop();
        assertThatThrownBy(() -> veilArbAbacService.helsesjekk()).hasCauseInstanceOf(ConnectException.class);
    }

    @Test
    public void harVeilederSkriveTilgangTilFnr() {
        givenThat(get(urlEqualTo("/subject/person?fnr=fnr&action=update"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThat(veilArbAbacService.harVeilederSkriveTilgangTilFnr(VEILEDER_ID, FNR)).isFalse();

        givenThat(get(urlEqualTo("/subject/person?fnr=fnr&action=update"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        assertThat(veilArbAbacService.harVeilederSkriveTilgangTilFnr(VEILEDER_ID, FNR)).isTrue();
    }

}