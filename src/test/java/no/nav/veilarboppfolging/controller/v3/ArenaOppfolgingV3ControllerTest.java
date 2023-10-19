package no.nav.veilarboppfolging.controller.v3;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.domain.PersonRequest;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.test.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArenaOppfolgingV3Controller.class)
public class ArenaOppfolgingV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArenaOppfolgingService arenaOppfolgingService;

    @MockBean
    private AuthService authService;

    @Test
    public void getOppfolgingsstatus__should_check_authorization() throws Exception {
        Fnr fnr = Fnr.of("123456");

        mockMvc.perform(post("/api/person/v3/oppfolgingsstatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fnr\":\""+fnr.get()+"\"}")
        ).andExpect(status().is(HttpStatus.OK_200));

        verify(authService, times(1)).sjekkLesetilgangMedFnr(fnr);
    }

    @Test
    public void getOppfolginsstatus__should_return_correct_data_and_status_code() throws Exception {
        Fnr fnr = Fnr.of("123456");
        PersonRequest personRequest = new PersonRequest();
        personRequest.setFnr(fnr);

        OppfolgingEnhetMedVeilederResponse response = new OppfolgingEnhetMedVeilederResponse()
                .setVeilederId("Z12345")
                .setFormidlingsgruppe("ARBS")
                .setServicegruppe("BKART")
                .setHovedmaalkode("BEHOLDEA")
                .setOppfolgingsenhet(new OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet("NAV Testheim", "1234"));

        String json = TestUtils.readTestResourceFile("controller/arena-oppfolging/get-oppfolgingsstatus-response.json");

        when(arenaOppfolgingService.getOppfolginsstatus(personRequest)).thenReturn(response);

        mockMvc.perform(post("/api/person/v3/oppfolgingsstatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fnr\":\""+fnr.get()+"\"}"))
                .andExpect(status().is(200))
                .andExpect(content().json(json, true));
    }

}
