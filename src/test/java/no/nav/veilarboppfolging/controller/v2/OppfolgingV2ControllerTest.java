package no.nav.veilarboppfolging.controller.v2;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OppfolgingV2Controller.class)
class OppfolgingV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private OppfolgingService oppfolgingService;

    @Test
    void hentGjeldendeOppfolginsperiode_should_return_gjeldende() throws Exception {
        ZonedDateTime startDato = ZonedDateTime.of(2021, 8, 27, 13, 44, 26, 356299000, ZoneId.of("Europe/Paris"));
        UUID uuid = UUID.fromString("e3e7f94b-d08d-464b-bdf5-e219207e915f");
        OppfolgingsperiodeEntity gjeldendePeriode = OppfolgingsperiodeEntity.builder()
                .aktorId("test1")
                .startDato(startDato)
                .sluttDato(null)
                .veileder("test")
                .uuid(uuid)
                .kvpPerioder(List.of(KvpPeriodeEntity.builder().aktorId("test2").build()))
                .build();

        Fnr fnr = Fnr.of("1234");
        when(oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)).thenReturn(Optional.of(gjeldendePeriode));

        String expectedJson = "{\"uuid\":\"e3e7f94b-d08d-464b-bdf5-e219207e915f\",\"startDato\":\"2021-08-27T13:44:26.356299+02:00\",\"sluttDato\":null}";

        mockMvc.perform(get("/api/v2/oppfolging/periode/gjeldende").queryParam("fnr", fnr.get()))
                .andExpect(content().json(expectedJson));
    }


    @Test
    void hentGJeldendeOppfolgingsPeriode_should_return_204_on_empty_result() throws Exception {
        Fnr fnr = Fnr.of("1234");
        when(oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v2/oppfolging/periode/gjeldende").queryParam("fnr", fnr.get()))
                .andExpect(status().is(HttpStatus.NO_CONTENT_204));
    }

    @Test
    void hentUnderOppfolgingMedFnr() throws Exception{
        Fnr fnr = Fnr.of("1234");
        when(oppfolgingService.erUnderOppfolging(fnr)).thenReturn(true);
        mockMvc.perform(get("/api/v2/oppfolging").queryParam("fnr", fnr.get()))
                .andExpect(status().is(HttpStatus.OK_200));
    }



}