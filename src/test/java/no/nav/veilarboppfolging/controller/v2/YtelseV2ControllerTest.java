package no.nav.veilarboppfolging.controller.v2;

import jakarta.ws.rs.core.MediaType;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.controller.v2.request.YtelserRequest;
import no.nav.veilarboppfolging.controller.v2.response.YtelserV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.YtelserOgAktiviteterService;
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static no.nav.veilarboppfolging.client.ytelseskontrakt.ActualYtelseskontraktResponse.getKomplettResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = YtelseV2Controller.class)
class YtelseV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    AuthorizationInterceptor authorizationInterceptor;

    @MockBean
    private YtelserOgAktiviteterService ytelserOgAktiviteterService;

    @Test
    void hentYtelser() throws Exception{
        Fnr fnr = Fnr.of("1234");
        YtelserRequest ytelserRequest = new YtelserRequest(fnr);
        YtelseskontraktResponse response = getKomplettResponse();
        YtelserV2Response response2 = new YtelserV2Response(response.getVedtaksliste(), response.getYtelser());
        when(authorizationInterceptor.preHandle(any(),any(),any())).thenReturn(true);
        when(ytelserOgAktiviteterService.hentYtelseskontrakt(fnr)).thenReturn(response);
        String expJson = JsonUtils.toJson(response2);
        mockMvc.perform(post("/api/v2/person/hent-ytelser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(ytelserRequest))
                )
                .andExpect(status().is(HttpStatus.OK_200))
                .andExpect(content().json(expJson));
    }

}