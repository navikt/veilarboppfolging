package no.nav.veilarboppfolging.controller;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarboppfolging.service.KafkaRepubliseringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.veilarboppfolging.test.TestUtils.verifiserAsynkront;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthContextHolder authContextHolder;

    @MockBean
    private KafkaRepubliseringService kafkaRepubliseringService;

    @Test
    public void republiserOppfolgingsperioder__should_return_401_if_user_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.empty());
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/oppfolgingsperioder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserOppfolgingsperioder__should_return_401_if_role_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/republiser/oppfolgingsperioder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserOppfolgingsperioder__should_return_403_if_not_pto_admin() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvmyapp"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/oppfolgingsperioder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserOppfolgingsperioder__should_return_403_if_not_system_user() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.EKSTERN));

        mockMvc.perform(post("/api/admin/republiser/oppfolgingsperioder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserOppfolgingsperioder__should_return_job_id_and_republish() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/oppfolgingsperioder"))
                .andExpect(status().is(200))
                .andExpect(content().string(matchesPattern("^([a-f0-9]+)$")));

        verifiserAsynkront(3, TimeUnit.SECONDS, () -> {
            verify(kafkaRepubliseringService, times(1)).republiserOppfolgingsperioder();
        });
    }



    @Test
    public void republiserTilordnetVeileder__should_return_401_if_user_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.empty());
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/tilordnet-veileder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserTilordnetVeileder__should_return_401_if_role_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/republiser/tilordnet-veileder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserTilordnetVeileder__should_return_403_if_not_pto_admin() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvmyapp"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/tilordnet-veileder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserTilordnetVeileder__should_return_403_if_not_system_user() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.EKSTERN));

        mockMvc.perform(post("/api/admin/republiser/tilordnet-veileder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserTilordnetVeileder__should_return_job_id_and_republish() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/tilordnet-veileder"))
                .andExpect(status().is(200))
                .andExpect(content().string(matchesPattern("^([a-f0-9]+)$")));

        verifiserAsynkront(3, TimeUnit.SECONDS, () -> {
            verify(kafkaRepubliseringService, times(1)).republiserTilordnetVeileder();
        });
    }


    @Test
    public void republiserEndringPaNyForVeileder__should_return_401_if_user_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.empty());
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/endring-pa-ny-for-veileder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserEndringPaNyForVeileder__should_return_401_if_role_missing() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/republiser/endring-pa-ny-for-veileder"))
                .andExpect(status().is(401));
    }

    @Test
    public void republiserEndringPaNyForVeileder__should_return_403_if_not_pto_admin() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvmyapp"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/endring-pa-ny-for-veileder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserEndringPaNyForVeileder__should_return_403_if_not_system_user() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.EKSTERN));

        mockMvc.perform(post("/api/admin/republiser/endring-pa-ny-for-veileder"))
                .andExpect(status().is(403));
    }

    @Test
    public void republiserEndringPaNyForVeileder__should_return_job_id_and_republish() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvpto-admin"));
        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.SYSTEM));

        mockMvc.perform(post("/api/admin/republiser/endring-pa-ny-for-veileder"))
                .andExpect(status().is(200))
                .andExpect(content().string(matchesPattern("^([a-f0-9]+)$")));

        verifiserAsynkront(3, TimeUnit.SECONDS, () -> {
            verify(kafkaRepubliseringService, times(1)).republiserEndringPaNyForVeileder();
        });
    }
}
