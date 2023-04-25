package no.nav.veilarboppfolging.controller.v2;

import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = ManuellStatusV2Controller.class)
public class ManuellStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private ManuellStatusService manuellStatusService;

    @Test
    public void synkroniserManuellStatusMedDigdir__should_only_be_used_by_internal_users() throws Exception {
        mockMvc.perform(post("/api/v2/manuell/synkroniser-med-digdir")
                .queryParam("fnr", TEST_FNR.get()));

        verify(authService, times(1)).skalVereInternBruker();
    }

    @Test
    public void synkroniserManuellStatusMedDigdir__should_synchronize() throws Exception {
        mockMvc.perform(post("/api/v2/manuell/synkroniser-med-digdir")
                .queryParam("fnr", TEST_FNR.get()));

        verify(manuellStatusService, times(1)).synkroniserManuellStatusMedDigdir(TEST_FNR);
    }


}
