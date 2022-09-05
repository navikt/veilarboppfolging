package no.nav.veilarboppfolging.controller;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
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

import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.VEILEDER_TILORDNET;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilOppfolgingPeriodeDTO;
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
    private ManuellStatusService manuellStatusService;

    @Test
    public void oppfolgingsperioder_skal_sjekke_at_bruker_er_systembruker() throws Exception {
        Fnr fnr = Fnr.of("1234");

        when(oppfolgingService.hentOppfolgingsperioder(eq(fnr))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/oppfolging/oppfolgingsperioder").queryParam("fnr", fnr.get()));

        verify(authService, times(1)).skalVereSystemBruker();
        verify(oppfolgingService, times(1)).hentOppfolgingsperioder(eq(fnr));
    }

    @Test
    public void oppfolgingsperioder_skal_returnere_oppfolgingsperioder() throws Exception {
        Fnr fnr = Fnr.of("1234");

        List<OppfolgingsperiodeEntity> perioder = new ArrayList<>();
        perioder.add(
                OppfolgingsperiodeEntity.builder()
                        .aktorId("test1")
                        .begrunnelse("begrunnelse")
                        .startDato(ZonedDateTime.now())
                        .sluttDato(ZonedDateTime.now().plusDays(1))
                        .veileder("test")
                        .kvpPerioder(List.of(KvpPeriodeEntity.builder().aktorId("test2").build()))
                        .build()
        );

        String expectedJson = JsonUtils.toJson(
                perioder.stream()
                        .map(op -> tilOppfolgingPeriodeDTO(op, true))
                        .collect(Collectors.toList())
        );

        when(oppfolgingService.hentOppfolgingsperioder(eq(fnr))).thenReturn(perioder);

        mockMvc.perform(get("/api/oppfolging/oppfolgingsperioder").queryParam("fnr", fnr.get()))
                .andExpect(content().json(expectedJson));
    }

    @Test
    public void innstillingshistorikk_skal_returnere_innstillingshistorikk() throws Exception {
        Fnr fnr = Fnr.of("1234");
        String veileder = "Veileder1";
        String tilordnetAvVeileder = "Veileder2";

        List<HistorikkHendelse> historikker = new ArrayList<>();
        historikker.add(
                HistorikkHendelse.builder()
                        .type(VEILEDER_TILORDNET)
                        .begrunnelse("Brukeren er tildelt veileder " + veileder)
                        .dato(ZonedDateTime.parse("2022-09-05T12:27:29.301343+02:00"))
                        .opprettetAv(NAV)
                        .opprettetAvBrukerId(tilordnetAvVeileder)
                        .build()
        );

        String expectedJson = "[{\"type\":\"VEILEDER_TILORDNET\",\"dato\":\"2022-09-05T12:27:29.301343+02:00\",\"begrunnelse\":\"Brukeren er tildelt veileder Veileder1\",\"opprettetAv\":\"NAV\",\"opprettetAvBrukerId\":\"Veileder2\",\"dialogId\":null,\"enhet\":null}]";

        when(historikkService.hentInstillingsHistorikk(eq(fnr))).thenReturn(historikker);

        mockMvc.perform(get("/api/oppfolging/innstillingsHistorikk").queryParam("fnr", fnr.get()))
                .andExpect(content().json(expectedJson));
    }
}
