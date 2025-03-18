package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static java.lang.String.format;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArenaOppfolgingController.class)
public class VeilarbArenaOppfolgingsStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArenaOppfolgingService arenaOppfolgingService;

    @MockitoBean
    private AuthService authService;

    @Test
    public void getOppfolginsstatus__should_return_correct_data_and_status_code() throws Exception {
        Fnr fnr = Fnr.of("123456");
        AktorId aktorId = AktorId.of("123456");

        var response = new OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet("NAV Testheim", "1234");

        String json = """
          { "navn": "NAV Testheim", "enhetId":  "1234"}
        """;

        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnr);
        when(arenaOppfolgingService.hentArenaOppfolgingsEnhet(fnr)).thenReturn(response);

        mockMvc.perform(get(format("/api/person/oppfolgingsenhet?aktorId=%s", aktorId)))
                .andExpect(status().is(200))
                .andExpect(content().json(json, true));
    }

}
