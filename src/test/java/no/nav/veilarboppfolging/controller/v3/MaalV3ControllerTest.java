package no.nav.veilarboppfolging.controller.v3;

import no.nav.common.json.JsonUtils;
import no.nav.veilarboppfolging.controller.v3.request.MaalForPersonRequest;
import no.nav.veilarboppfolging.controller.v3.request.MaalRequest;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MaalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.veilarboppfolging.test.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = MaalV3Controller.class)
public class MaalV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaalService maalService;

    @MockBean
    private AuthService authService;

    @Test
    public void hentMaal_skal_returnere_maal() throws Exception {
        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.hentMal(any())).thenReturn(
                new MaalEntity()
                        .setMal("Mitt mål")
                        .setEndretAv(TEST_NAV_IDENT.get())
                        .setDato(ZonedDateTime.parse("2022-11-03T10:00:00+01:00"))
        );

        String expectedJson = "{\"mal\":\"Mitt mål\",\"endretAv\":\"VEILEDER\",\"dato\":\"2022-11-03T10:00:00+01:00\"}";

        mockMvc.perform(
                post("/api/v3/hent-maal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new MaalForPersonRequest(TEST_FNR)))
        ).andExpect(content().json(expectedJson));
    }

    @Test
    public void hentMaalListe_skal_returnere_liste_av_maal() throws Exception {
        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.hentMaalList(any())).thenReturn(
                List.of(
                        new MaalEntity()
                                .setMal("Mitt mål")
                                .setEndretAv(TEST_NAV_IDENT.get())
                                .setDato(ZonedDateTime.parse("2022-11-03T10:00:00+01:00")),
                        new MaalEntity()
                                .setAktorId(TEST_AKTOR_ID.get())
                                .setMal("Mitt andre mål")
                                .setEndretAv(TEST_AKTOR_ID.get())
                                .setDato(ZonedDateTime.parse("2023-08-03T14:00:00+01:00"))
                )
        );

        String expectedJson = "[{\"mal\":\"Mitt mål\",\"endretAv\":\"VEILEDER\",\"dato\":\"2022-11-03T10:00:00+01:00\"},{\"mal\":\"Mitt andre mål\",\"endretAv\":\"BRUKER\",\"dato\":\"2023-08-03T14:00:00+01:00\"}]";

        mockMvc.perform(
                post("/api/v3/maal/hent-alle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new MaalForPersonRequest(TEST_FNR)))
        ).andExpect(content().json(expectedJson));
    }

    @Test
    public void oppdaterMaal_skal_returnere_oppdatert_maal() throws Exception {
        MaalEntity oppdatertMaal = new MaalEntity()
                .setAktorId(TEST_AKTOR_ID.get())
                .setMal("Oppdatert mål")
                .setEndretAv(TEST_AKTOR_ID.get())
                .setDato(ZonedDateTime.parse("2023-09-12T08:00:00+01:00"));

        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.oppdaterMaal(any(), any(), any())).thenReturn(oppdatertMaal);

        String expectedJson = "{\"mal\":\"Oppdatert mål\",\"endretAv\":\"BRUKER\",\"dato\":\"2023-09-12T08:00:00+01:00\"}";

        mockMvc.perform(
                post("/api/v3/maal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(
                                new MaalRequest(
                                        TEST_FNR,
                                        "Oppdatert mål",
                                        TEST_AKTOR_ID.get(),
                                        ZonedDateTime.parse("2023-09-12T08:00:00+01:00")
                                )
                        ))
        ).andExpect(content().json(expectedJson));
    }
}
