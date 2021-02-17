package no.nav.veilarboppfolging.controller;

import no.nav.common.json.JsonUtils;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.veilarboppfolging.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarboppfolging.utils.DtoMappers.tilDTO;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = OppfolgingController.class)
public class OppfolgingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private OppfolgingService oppfolgingService;

    @MockBean
    private KvpService kvpService;

    @MockBean
    private HistorikkService historikkService;

    @MockBean
    private EskaleringService eskaleringService;

    @MockBean
    private ManuellStatusService manuellStatusService;

    @Test
    public void oppfolgingsperioder_skal_sjekke_at_bruker_er_systembruker() throws Exception {
        String fnr = "1234";

        when(oppfolgingService.hentOppfolgingsperioder(eq(fnr))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/oppfolging/oppfolgingsperioder").queryParam("fnr", fnr));

        verify(authService, times(1)).skalVereSystemBruker();
        verify(oppfolgingService, times(1)).hentOppfolgingsperioder(eq(fnr));
    }

    @Test
    public void oppfolgingsperioder_skal_returnere_oppfolgingsperioder() throws Exception {
        String fnr = "1234";

        List<Oppfolgingsperiode> perioder = new ArrayList<>();
        perioder.add(
                Oppfolgingsperiode.builder()
                        .aktorId("test1")
                        .begrunnelse("begrunnelse")
                        .startDato(ZonedDateTime.now())
                        .sluttDato(ZonedDateTime.now().plusDays(1))
                        .veileder("test")
                        .kvpPerioder(List.of(Kvp.builder().aktorId("test2").build()))
                        .build()
        );

        String expectedJson = JsonUtils.toJson(
                perioder.stream()
                .map(op -> tilDTO(op, true))
                .collect(Collectors.toList())
        );

        when(oppfolgingService.hentOppfolgingsperioder(eq(fnr))).thenReturn(perioder);

        mockMvc.perform(get("/api/oppfolging/oppfolgingsperioder").queryParam("fnr", fnr))
                .andExpect(content().json(expectedJson));
    }

}
