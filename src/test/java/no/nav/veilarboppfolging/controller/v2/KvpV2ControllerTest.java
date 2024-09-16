package no.nav.veilarboppfolging.controller.v2;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.controller.response.KvpDTO;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KvpService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: Må ha som mål å skrive om testkoden i appen til å faktisk gjøre HTTP-kall og så endre disse testcasene
@WebMvcTest(controllers = KvpV2Controller.class)
public class KvpV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KvpRepository kvpRepository;

    @MockBean
    private KvpService kvpService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthContextHolder authContextHolder;

    private static final AktorId AKTOR_ID = AktorId.of("1234");
    private static final long KVP_ID = 1L;
    private static final String ENHET_ID = "0001";

    @Test
    public void user_missing() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(authService).authorizeRequest(any(), any());

        mockMvc.perform(get("/api/v2/kvp").queryParam("aktorId", AKTOR_ID.get()))
                .andExpect(status().is(401));
    }

    @Test
    public void unauthorized_user_() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(authService).authorizeRequest(any(), any());

        mockMvc.perform(get("/api/v2/kvp").queryParam("aktorId", AKTOR_ID.get()))
                .andExpect(status().is(401));
    }

    @Test
    public void no_active_kvp() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvveilarbdialog"));
        when(authService.erSystemBruker()).thenReturn(true);

        when(kvpRepository.gjeldendeKvp(any())).thenReturn(0L);

        mockMvc.perform(get("/api/v2/kvp").queryParam("aktorId", AKTOR_ID.get()))
                .andExpect(content().string(""))
                .andExpect(status().is(204));
    }

    @Test
    public void with_active_kvp() throws Exception {
        when(authContextHolder.getSubject()).thenReturn(Optional.of("srvveilarbdialog"));
        when(authService.erSystemBruker()).thenReturn(true);

        when(kvpRepository.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepository.hentKvpPeriode(KVP_ID)).thenReturn(Optional.of(kvp()));

        KvpDTO expectedKvp = DtoMappers.kvpToDTO(kvp());

		when(kvpService.hentGjeldendeKvpPeriode(any())).thenReturn(Optional.of(kvp()));

        mockMvc.perform(get("/api/v2/kvp").queryParam("aktorId", AKTOR_ID.get()))
                .andExpect(content().string(JsonUtils.toJson(expectedKvp)))
                .andExpect(status().is(200));
    }

    @Test
    public void with_no_active_kvp() throws Exception {
		when(authContextHolder.getSubject()).thenReturn(Optional.of("srvveilarbdialog"));
		when(authService.erSystemBruker()).thenReturn(true);

		when(kvpRepository.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
		when(kvpRepository.hentKvpPeriode(KVP_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v2/kvp").queryParam("aktorId", AKTOR_ID.get()))
				.andExpect(content().string(""))
				.andExpect(status().is(204));
    }

    private KvpPeriodeEntity kvp() {
        return KvpPeriodeEntity.builder()
                .kvpId(KVP_ID)
                .aktorId(AKTOR_ID.get())
                .opprettetDato(ZonedDateTime.of(2020, 5, 4, 3, 2, 1, 0, ZoneId.systemDefault()))
                .enhet(ENHET_ID)
                .build();
    }

}
