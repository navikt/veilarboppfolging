package no.nav.veilarboppfolging.controller.v2;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = VeilederV2Controller.class)
public class VeilederV2ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    public VeilederTilordningService veilederTilordningService;

    @MockitoBean
    private AuthService authService;
    @Test
    public void hent_veileder_skal_returnere_riktig_veileder() throws Exception {
        NavIdent navIdent = NavIdent.of("1234");
        when(veilederTilordningService.hentTilordnetVeilederIdent(any())).thenReturn(Optional.of(navIdent));
        String expJson = "{\"veilederIdent\":\"1234\"}";
        mockMvc.perform(get("/api/v2/veileder").queryParam("fnr", String.valueOf(navIdent)))
                .andExpect(content().json(expJson));
    }
    @Test
    public void hent_veileder_skal_returnere_riktig_veileder_2() throws Exception {
        AktorId aktorId = AktorId.of("1234");
        NavIdent navIdent = NavIdent.of("1234");
        when(veilederTilordningService.hentTilordnetVeilederIdent(any())).thenReturn(Optional.of(navIdent));
        String expJson = "{\"veilederIdent\":\"1234\"}";
        mockMvc.perform(get("/api/v2/veileder").queryParam("aktorId", String.valueOf(aktorId)))
                .andExpect(content().json(expJson));
    }
}
