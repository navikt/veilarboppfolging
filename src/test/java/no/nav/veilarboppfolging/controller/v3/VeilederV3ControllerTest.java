package no.nav.veilarboppfolging.controller.v3;


import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.controller.v3.request.VeilederRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = VeilederV3Controller.class)
public class VeilederV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    public VeilederTilordningService veilederTilordningService;

    @MockBean
    private AuthService authService;


    @Test
    public void hent_veileder_skal_returnere_riktig_veileder() throws Exception {
        NavIdent navIdent = NavIdent.of("1234");
        VeilederRequest veilederRequest = new VeilederRequest(Fnr.of("5678"));
        when(veilederTilordningService.hentTilordnetVeilederIdent(any())).thenReturn(Optional.of(navIdent));
        String expJson = "{\"veilederIdent\":\"1234\"}";
        mockMvc.perform(post("/api/v3/hent-veileder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(veilederRequest))).andExpect(content().json(expJson));


    }

}
