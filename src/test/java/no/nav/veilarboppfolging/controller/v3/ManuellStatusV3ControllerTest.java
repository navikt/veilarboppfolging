package no.nav.veilarboppfolging.controller.v3;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.v3.request.ManuellStatusRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = ManuellStatusV3Controller.class)
public class ManuellStatusV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private ManuellStatusService manuellStatusService;


    ManuellStatusRequest manuellStatusRequest = new ManuellStatusRequest(TEST_FNR);

    @Test
    public void hent_manuell_skal_returnere_true() throws Exception {
        when(manuellStatusService.erManuell((Fnr) any())).thenReturn(true);
        String expJson = "{\"erUnderManuellOppfolging\":true}";
        mockMvc.perform(post("/api/v3/hent-manuell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(manuellStatusRequest))).andExpect(content().json(expJson));

    }

    @Test
    public void synkroniserManuellStatusMedDigdir__should_only_be_used_by_internal_users() throws Exception {
        mockMvc.perform(post("/api/v3/manuell/synkroniser-med-dkif")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(manuellStatusRequest))
        );

        verify(authService, times(1)).skalVereInternBruker();
    }

    @Test
    public void synkroniserManuellStatusMedDigdir__should_synchronize() throws Exception {
        mockMvc.perform(post("/api/v3/manuell/synkroniser-med-dkif")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(manuellStatusRequest))
        );

        verify(manuellStatusService, times(1)).synkroniserManuellStatusMedDigdir(TEST_FNR);
    }


}
