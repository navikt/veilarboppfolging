package no.nav.veilarboppfolging.controller.v3;

import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OppfolgingV3Controller.class)
public class OppfolgingV3ControllerTest{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private OppfolgingService oppfolgingService;

    @MockBean
    AuthorizationInterceptor authorizationInterceptor;

    @Test
    void hentUnderOppfolgingV3() throws Exception {
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getInnloggetBrukerIdent()).thenReturn("1234");
        mockMvc.perform(post("/api/v3/oppfolging"))
                .andExpect(status().is(HttpStatus.OK_200));
    }
}
