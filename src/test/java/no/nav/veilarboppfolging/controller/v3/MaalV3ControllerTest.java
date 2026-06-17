package no.nav.veilarboppfolging.controller.v3;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MaalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.veilarboppfolging.test.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = MaalV3Controller.class)
public class MaalV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaalService maalService;

    @MockitoBean
    private AuthService authService;

    private MaalEntity lagMaal(String aktorId, String mal, String endretAv, ZonedDateTime dato) {
        return new MaalEntity(
                null,
                aktorId,
                mal,
                endretAv,
                dato
        );
    }

    @Test
    public void hentMaal_skal_returnere_maal() throws Exception {
        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.hentMal(any())).thenReturn(
                Optional.of(lagMaal(
                        TEST_AKTOR_ID.get(),
                        "Mitt mål",
                        TEST_NAV_IDENT.get(),
                        ZonedDateTime.parse("2022-11-03T10:00:00+01:00")
                ))
        );

        String expectedJson = "{\"mal\":\"Mitt mål\",\"endretAv\":\"VEILEDER\",\"dato\":\"2022-11-03T10:00:00+01:00\"}";

        mockMvc.perform(
                post("/api/v3/hent-maal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
        ).andExpect(content().json(expectedJson));
    }

    @Test
    public void hentMaalListe_skal_returnere_liste_av_maal() throws Exception {
        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.hentMaalList(any())).thenReturn(
                List.of(
                        lagMaal(
                                TEST_AKTOR_ID.get(),
                                "Mitt mål",
                                TEST_NAV_IDENT.get(),
                                ZonedDateTime.parse("2022-11-03T10:00:00+01:00")
                        ),
                        lagMaal(
                                TEST_AKTOR_ID.get(),
                                "Mitt andre mål",
                                TEST_AKTOR_ID.get(),
                                ZonedDateTime.parse("2023-08-03T14:00:00+01:00")
                        )
                )
        );

        String expectedJson = "[{\"mal\":\"Mitt mål\",\"endretAv\":\"VEILEDER\",\"dato\":\"2022-11-03T10:00:00+01:00\"},{\"mal\":\"Mitt andre mål\",\"endretAv\":\"BRUKER\",\"dato\":\"2023-08-03T14:00:00+01:00\"}]";

        mockMvc.perform(
                post("/api/v3/maal/hent-alle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
        ).andExpect(content().json(expectedJson));
    }

    @Test
    public void oppdaterMaal_skal_returnere_oppdatert_maal() throws Exception {
        MaalEntity oppdatertMaal = lagMaal(
                TEST_AKTOR_ID.get(),
                "Oppdatert mål",
                TEST_AKTOR_ID.get(),
                ZonedDateTime.parse("2023-09-12T08:00:00+01:00")
        );

        when(authService.hentIdentForEksternEllerIntern(any())).thenReturn(TEST_FNR);
        when(maalService.oppdaterMaal(any(), any(), any())).thenReturn(oppdatertMaal);

        String expectedJson = "{\"mal\":\"Oppdatert mål\",\"endretAv\":\"BRUKER\",\"dato\":\"2023-09-12T08:00:00+01:00\"}";

        mockMvc.perform(
                post("/api/v3/maal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\",\"maalInnhold\":{\"maal\":\"Oppdatert mål\",\"endretAv\":\"11122233334445\",\"dato\":\"2023-09-12T08:00:00+01:00\"}}")
        ).andExpect(content().json(expectedJson));
    }
}
