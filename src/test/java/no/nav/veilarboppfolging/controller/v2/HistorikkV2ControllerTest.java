package no.nav.veilarboppfolging.controller.v2;

import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.HistorikkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.OPPFOLGINGSENHET_ENDRET;
import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.VEILEDER_TILORDNET;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;
import static no.nav.veilarboppfolging.test.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = HistorikkV2Controller.class)
public class HistorikkV2ControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    private HistorikkService historikkService;

    @MockBean
    private AuthService authService;

    @Test
    public void hentInnstillingsHistorikk_skal_returnere_historikk_hendelse_liste() throws Exception {

        when(historikkService.hentInstillingsHistorikk(any())).thenReturn(List.of(
                HistorikkHendelse.builder()
                        .type(VEILEDER_TILORDNET)
                        .begrunnelse("Brukeren er tildelt veileder " + TEST_NAV_IDENT.get())
                        .dato(ZonedDateTime.parse("2022-11-03T10:00:00+01:00"))
                        .opprettetAv(NAV)
                        .opprettetAvBrukerId(TEST_NAV_IDENT.get())
                        .build(),
                HistorikkHendelse.builder()
                        .type(OPPFOLGINGSENHET_ENDRET)
                        .enhet(TEST_ENHET_ID.get())
                        .begrunnelse("Ny oppfølgingsenhet " + TEST_ENHET_ID.get())
                        .dato(ZonedDateTime.parse("2022-07-01T13:00:00+01:00"))
                        .opprettetAv(SYSTEM)
                        .build()
        ));

        String expectedJson = "[{\"type\":\"VEILEDER_TILORDNET\",\"begrunnelse\":\"Brukeren er tildelt veileder Z112233\",\"dato\":\"2022-11-03T10:00:00+01:00\",\"opprettetAv\":\"NAV\",\"opprettetAvBrukerId\":\"Z112233\"},{\"type\":\"OPPFOLGINGSENHET_ENDRET\",\"begrunnelse\":\"Ny oppfølgingsenhet 0123\",\"dato\":\"2022-07-01T13:00:00+01:00\",\"opprettetAv\":\"SYSTEM\"}]";

        mockMvc.perform(
                get("/api/v2/historikk").queryParam("fnr", String.valueOf(TEST_FNR))
        ).andExpect(content().json(expectedJson));
    }
}
