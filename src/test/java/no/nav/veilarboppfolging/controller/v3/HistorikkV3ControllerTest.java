package no.nav.veilarboppfolging.controller.v3;

import no.nav.common.json.JsonUtils;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.controller.v3.request.HistorikkRequest;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.HistorikkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.OPPFOLGINGSENHET_ENDRET;
import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.VEILEDER_TILORDNET;
import static no.nav.veilarboppfolging.test.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = HistorikkV3Controller.class)
public class HistorikkV3ControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private HistorikkService historikkService;

    @MockitoBean
    private AuthService authService;

    @Test
    public void hentInnstillingsHistorikk_skal_returnere_historikk_hendelse_liste() throws Exception {

        when(historikkService.hentInstillingsHistorikk(any())).thenReturn(List.of(
                HistorikkHendelse.builder()
                        .type(VEILEDER_TILORDNET)
                        .begrunnelse("Brukeren er tildelt veileder " + TEST_NAV_IDENT.get())
                        .dato(ZonedDateTime.parse("2022-11-03T10:00:00+01:00"))
                        .opprettetAv(KodeverkBruker.NAV)
                        .opprettetAvBrukerId(TEST_NAV_IDENT.get())
                        .build(),
                HistorikkHendelse.builder()
                        .type(OPPFOLGINGSENHET_ENDRET)
                        .enhet(TEST_ENHET_ID.get())
                        .begrunnelse("Ny oppfølgingsenhet " + TEST_ENHET_ID.get())
                        .dato(ZonedDateTime.parse("2022-07-01T13:00:00+01:00"))
                        .opprettetAv(KodeverkBruker.SYSTEM)
                        .build()
        ));

        String expectedJson = "[{\"type\":\"VEILEDER_TILORDNET\",\"begrunnelse\":\"Brukeren er tildelt veileder Z112233\",\"dato\":\"2022-11-03T10:00:00+01:00\",\"opprettetAv\":\"NAV\",\"opprettetAvBrukerId\":\"Z112233\"},{\"type\":\"OPPFOLGINGSENHET_ENDRET\",\"begrunnelse\":\"Ny oppfølgingsenhet 0123\",\"dato\":\"2022-07-01T13:00:00+01:00\",\"opprettetAv\":\"SYSTEM\"}]";

        mockMvc.perform(
                post("/api/v3/hent-instillingshistorikk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new HistorikkRequest(TEST_FNR)))
        ).andExpect(content().json(expectedJson));
    }
}
