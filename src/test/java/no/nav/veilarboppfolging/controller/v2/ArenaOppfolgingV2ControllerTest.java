package no.nav.veilarboppfolging.controller.v2;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.test.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArenaOppfolgingV2Controller.class)
public class ArenaOppfolgingV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArenaOppfolgingService arenaOppfolgingService;

    @MockBean
    private AuthService authService;

    @Test
    public void getOppfolgingsstatus__should_check_authorization() throws Exception {
        Fnr fnr = Fnr.of("123456");

        mockMvc.perform(post("/api/v2/person/hent-oppfolgingsstatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fnr\":\""+fnr.get()+"\"}"));

        verify(authService, times(1)).sjekkLesetilgangMedFnr(fnr);
    }

    @Test
    public void getOppfolgingsstatus__should_return_correct_data_and_status_code() throws Exception {
        Fnr fnr = Fnr.of("123456");

        OppfolgingEnhetMedVeilederResponse response = new OppfolgingEnhetMedVeilederResponse()
                .setVeilederId("Z12345")
                .setFormidlingsgruppe("ARBS")
                .setServicegruppe("BKART")
                .setHovedmaalkode("BEHOLDEA")
                .setOppfolgingsenhet(new OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet("NAV Testheim", "1234"));

        String json = TestUtils.readTestResourceFile("controller/arena-oppfolging/get-oppfolgingsstatus-response.json");

        when(arenaOppfolgingService.getOppfolginsstatus(fnr)).thenReturn(response);

        mockMvc.perform(post("/api/v2/person/hent-oppfolgingsstatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\""+fnr.get()+"\"}"))
                .andExpect(status().is(200))
                .andExpect(content().json(json, true));
    }

}
