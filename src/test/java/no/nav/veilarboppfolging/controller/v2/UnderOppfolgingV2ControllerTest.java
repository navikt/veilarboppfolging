package no.nav.veilarboppfolging.controller.v2;

import jakarta.ws.rs.core.MediaType;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.v2.request.UnderOppfolgingRequest;
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UnderOppfolgingV2Controller.class)
class UnderOppfolgingV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    AuthorizationInterceptor authorizationInterceptor;

    @MockBean
    private OppfolgingService oppfolgingService;


    @Test
    void hentUnderOppfolgingMedFnr() throws Exception{
        Fnr fnr = Fnr.of("1234");
        UnderOppfolgingRequest underOppfolgingRequest = new UnderOppfolgingRequest(fnr);
        UnderOppfolgingV2Response response = new UnderOppfolgingV2Response(true);
        when(oppfolgingService.erUnderOppfolging(fnr)).thenReturn(response.isErUnderOppfolging());
        mockMvc.perform(post("/api/v2/hent-underOppfolging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(underOppfolgingRequest))
                )
                .andExpect(status().is(HttpStatus.OK_200));
    }

    @Test
    void hentUnderOppfolgingUtenParams() throws Exception {
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getInnloggetBrukerIdent()).thenReturn("1234");
        when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        mockMvc.perform(post("/api/v2/hent-underOppfolging"))
                .andExpect(status().is(HttpStatus.OK_200));
    }
}