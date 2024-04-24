package no.nav.veilarboppfolging.controller.v3;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.controller.v3.request.OppfolgingRequest;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.service.*;
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.veilarboppfolging.test.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OppfolgingV3Controller.class)
class OppfolgingV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    AuthorizationInterceptor authorizationInterceptor;

    @MockBean
    private OppfolgingService oppfolgingService;

    @MockBean
    private ManuellStatusService manuellStatusService;

    @MockBean
    private KvpService kvpService;

    @MockBean
    private AktiverBrukerService aktiverBrukerService;

    @BeforeEach
    void setup() throws Exception {
        when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void hentUnderOppfolgingMedFnr_skal_returnere_underoppfolgingrespons() throws Exception {
        Fnr fnr = Fnr.of("1234");
        OppfolgingRequest oppfolgingRequest = new OppfolgingRequest(fnr);
        when(oppfolgingService.erUnderOppfolging(fnr)).thenReturn(true);

        String expectedJson = "{\"erUnderOppfolging\":true}";
        mockMvc.perform(post("/api/v3/hent-oppfolging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(oppfolgingRequest))
                )
                .andExpect(status().is(HttpStatus.OK_200))
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void hentBrukerInfo_skal_returnere_bruker() throws Exception {
        when(authService.getInnloggetBrukerIdent()).thenReturn(TEST_NAV_IDENT.get());
        when(authService.erInternBruker()).thenReturn(true);
        when(authService.erEksternBruker()).thenReturn(false);

        String expectedJson = "{\"id\":\"Z112233\",\"erVeileder\":true,\"erBruker\":false}";
        mockMvc.perform(get("/api/v3/oppfolging/me"))
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void hentOppfolgingsStatus_skal_returnere_oppfolgingstatus() throws Exception {
        when(authService.erInternBruker()).thenReturn(true);
        when(authService.hentIdentForEksternEllerIntern(TEST_FNR)).thenReturn(TEST_FNR);
        when(oppfolgingService.hentOppfolgingsStatus(TEST_FNR)).thenReturn(
                new OppfolgingStatusData()
                        .setFnr(TEST_FNR.get())
                        .setAktorId(TEST_AKTOR_ID.get())
                        .setUnderOppfolging(true)
                        .setManuell(false)
                        .setReservasjonKRR(false)
                        .setRegistrertKRR(true)
                        .setOppfolgingsperioder(Collections.emptyList())
                        .setKanReaktiveres(false)
                        .setInaktiveringsdato(null)
                        .setErIkkeArbeidssokerUtenOppfolging(false)
                        .setErSykmeldtMedArbeidsgiver(false)
                        .setHarSkriveTilgang(true)
                        .setServicegruppe("servicegruppe")
                        .setFormidlingsgruppe("formidlingsgruppe")
                        .setRettighetsgruppe("rettighetsgruppe")
                        .setKanVarsles(true)
                        .setUnderKvp(false)
        );

        String expectedJson = "{\"fnr\":\"12345678900\",\"aktorId\":\"11122233334445\",\"veilederId\":null,\"reservasjonKRR\":false,\"registrertKRR\":true,\"kanVarsles\":true,\"manuell\":false,\"underOppfolging\":true,\"underKvp\":false,\"oppfolgingUtgang\":null,\"kanStarteOppfolging\":false,\"avslutningStatus\":null,\"oppfolgingsPerioder\":[],\"harSkriveTilgang\":true,\"inaktivIArena\":null,\"kanReaktiveres\":false,\"inaktiveringsdato\":null,\"erSykmeldtMedArbeidsgiver\":false,\"servicegruppe\":\"servicegruppe\",\"formidlingsgruppe\":\"formidlingsgruppe\",\"rettighetsgruppe\":\"rettighetsgruppe\",\"erIkkeArbeidssokerUtenOppfolging\":false}";

        mockMvc.perform(post("/api/v3/oppfolging/hent-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void hentAvslutningStatus_skal_returnere_avslutningstatus() throws Exception {
        when(oppfolgingService.hentAvslutningStatus(TEST_FNR)).thenReturn(
                AvslutningStatusData.builder()
                        .kanAvslutte(true)
                        .underOppfolging(true)
                        .harYtelser(false)
                        .underKvp(false)
                        .inaktiveringsDato(LocalDate.parse("2023-01-01"))
                        .erIserv(false)
                        .build()
        );

        String expectedJson = "{\"kanAvslutte\":true,\"underOppfolging\":true,\"harYtelser\":false,\"underKvp\":false,\"inaktiveringsDato\":\"2023-01-01\",\"erIserv\":false}";
        mockMvc.perform(post("/api/v3/oppfolging/hent-avslutning-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andDo((result -> {
                    result.getRequest();
                }))
                .andExpect(status().is(200))
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void hentGjeldendeOppfolginsperiode_should_return_gjeldende() throws Exception {
        Fnr fnr = Fnr.of("1234");
        OppfolgingRequest oppfolgingRequest = new OppfolgingRequest(fnr);
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

        when(oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)).thenReturn(Optional.of(gjeldendePeriode));

        String expectedJson = "{\"uuid\":\"e3e7f94b-d08d-464b-bdf5-e219207e915f\",\"startDato\":\"2021-08-27T13:44:26.356299+02:00\",\"sluttDato\":null}";
        mockMvc.perform(post("/api/v3/oppfolging/hent-gjeldende-periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(oppfolgingRequest))
                )
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void hentGJeldendeOppfolgingsPeriode_should_return_204_on_empty_result() throws Exception {
        Fnr fnr = Fnr.of("1234");
        OppfolgingRequest oppfolgingRequest = new OppfolgingRequest(fnr);

        when(oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v3/oppfolging/hent-gjeldende-periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(oppfolgingRequest))
                )
                .andExpect(status().is(HttpStatus.NO_CONTENT_204));
    }

    @Test
    void hentOppfolgingsperioder_skal_returnere_oppfolgingsperioder() throws Exception {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingService.hentOppfolgingsperioder(TEST_AKTOR_ID)).thenReturn(
                List.of(
                        OppfolgingsperiodeEntity.builder()
                                .aktorId(TEST_AKTOR_ID.get())
                                .veileder(TEST_NAV_IDENT.get())
                                .begrunnelse("En begrunnelse")
                                .uuid(UUID.fromString("375faf4d-20b0-4a9d-bb44-a582de54fb58"))
                                .startDato(ZonedDateTime.parse("2023-04-06T16:00:00+01:00[Europe/Oslo]"))
                                .sluttDato(null)
                                .kvpPerioder(Collections.emptyList())
                                .build(),
                        OppfolgingsperiodeEntity.builder()
                                .aktorId(TEST_AKTOR_ID.get())
                                .veileder(TEST_NAV_IDENT.get())
                                .begrunnelse("En begrunnelse")
                                .uuid(UUID.fromString("76c69158-f1e8-4c53-897c-656583638a8d"))
                                .startDato(ZonedDateTime.parse("2022-01-06T16:00:00+01:00[Europe/Oslo]"))
                                .sluttDato(ZonedDateTime.parse("2022-06-06T16:00:00+01:00[Europe/Oslo]"))
                                .kvpPerioder(Collections.emptyList())
                                .build()
                )
        );

        String expectedJson = "[{\"uuid\":\"375faf4d-20b0-4a9d-bb44-a582de54fb58\",\"aktorId\":\"11122233334445\",\"veileder\":\"Z112233\",\"startDato\":\"2023-04-06T17:00:00+02:00\",\"sluttDato\":null,\"begrunnelse\":\"En begrunnelse\",\"kvpPerioder\":[]},{\"uuid\":\"76c69158-f1e8-4c53-897c-656583638a8d\",\"aktorId\":\"11122233334445\",\"veileder\":\"Z112233\",\"startDato\":\"2022-01-06T16:00:00+01:00\",\"sluttDato\":\"2022-06-06T17:00:00+02:00\",\"begrunnelse\":\"En begrunnelse\",\"kvpPerioder\":[]}]";
        mockMvc.perform(post("/api/v3/oppfolging/hent-perioder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void settTilManuell_skal_returnere_tom_respons() throws Exception {
        mockMvc.perform(post("/api/v3/oppfolging/settManuell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(204));
    }

    @Test
    void settTilDigital_skal_returnere_tom_respons() throws Exception {
        mockMvc.perform(post("/api/v3/oppfolging/settDigital")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(204));
    }

    @Test
    void startKvp_skal_returnere_tom_respons() throws Exception {
        mockMvc.perform(post("/api/v3/oppfolging/startKvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(204));
    }

    @Test
    void stoppKvp_skal_returnere_tom_respons() throws Exception {
        mockMvc.perform(post("/api/v3/oppfolging/stoppKvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(204));
    }

    @Test
    void hentVeilederTilgang_skal_returnere_veiledertilgang() throws Exception {
        when(oppfolgingService.hentVeilederTilgang(TEST_FNR)).thenReturn(new VeilederTilgang(true));

        String expectedJson = "{\"tilgangTilBrukersKontor\":true}";
        mockMvc.perform(post("/api/v3/oppfolging/hent-veilederTilgang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fnr\":\"12345678900\"}")
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void harFlereAktorIderMedOppfolging_skal_returnere_true() throws Exception {
        when(authService.hentIdentForEksternEllerIntern(TEST_FNR)).thenReturn(TEST_FNR);
        when(oppfolgingService.hentHarFlereAktorIderMedOppfolging(TEST_FNR)).thenReturn(true);

        String expectedJson = "true";
        mockMvc.perform(post("/api/v3/oppfolging/harFlereAktorIderMedOppfolging")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fnr\":\"12345678900\"}")
        )
                .andExpect(status().is(200))
                .andExpect(content().string(expectedJson));
    }

    @Test
    void aktiverSykmeldt_skal_returnere_tom_respons() throws Exception {
        mockMvc.perform(post("/api/v3/oppfolging/aktiverSykmeldt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fnr\":\"12345678900\",\"sykmeldtBrukerType\":\"SKAL_TIL_SAMME_ARBEIDSGIVER\"}")
        )
                .andExpect(status().is(204));
    }
}
