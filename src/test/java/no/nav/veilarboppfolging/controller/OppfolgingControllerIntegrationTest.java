package no.nav.veilarboppfolging.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.controller.response.AvslutningStatus;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.controller.v2.OppfolgingV2Controller;
import no.nav.veilarboppfolging.controller.v2.request.AvsluttOppfolgingV2Request;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.service.ArenaYtelserService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class OppfolgingControllerIntegrationTest extends IntegrationTest {

    private final static Fnr FNR = Fnr.of("12345678901");
    private final static AktorId AKTOR_ID = AktorId.of("09876543210987");
    private final static UUID veilederUUID = UUID.randomUUID();
    private final static String veilederIdent = "Z999999";
    private final static UUID sub = UUID.randomUUID();

    @Autowired
    AktorOppslagClient aktorOppslagClient;

    @Autowired
    AuthService authService;

    @MockitoBean
    ArenaYtelserService arenaYtelserService;

    @Autowired
    VeilarbarenaClient veilarbarenaClient;

    @Autowired
    OppfolgingController oppfolgingController;

    @Autowired
    OppfolgingV2Controller oppfolgingV2Controller;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    PoaoTilgangClient poaoTilgangClient;

    @Autowired
    AmtDeltakerClient amtDeltakerClient;

    @Test
    void hentOppfolgingsPeriode_brukerHarEnAktivOppfolgingsPeriode() throws EmptyResultDataAccessException {
        mockAuthOk();

        var perioder = startOppfolging();

        Assertions.assertEquals(1, perioder.size());

        var policyInput = new NavAnsattTilgangTilEksternBrukerPolicyInput(veilederUUID, TilgangType.LESE, FNR.get());
        ApiResult<Decision> permit = ApiResult.Companion.success(Decision.Permit.INSTANCE);
        doReturn(permit).when(poaoTilgangClient).evaluatePolicy(policyInput);

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid;
        var periode = oppfolgingController.hentOppfolgingsPeriode(uuid.toString());

        Assertions.assertEquals(uuid, periode.getUuid());
        Assertions.assertNotNull(forstePeriode.startDato);
        Assertions.assertEquals(forstePeriode.startDato, periode.getStartDato());
    }

    @Test
    void hentOppfolgingsPeriode_veilederManglerTilgang() {
        mockAuthOk();
        var perioder = startOppfolging();

        Assertions.assertEquals(1, perioder.size());

        var forstePeriode = perioder.get(0);
        var uuid = forstePeriode.uuid.toString();

        var policyInput = new NavAnsattTilgangTilEksternBrukerPolicyInput(veilederUUID, TilgangType.LESE, FNR.get());
        ApiResult<Decision> deny = ApiResult.Companion.success(new Decision.Deny("Nei", "Fordi"));
        doReturn(deny).when(poaoTilgangClient).evaluatePolicy(policyInput);

        assertThrows(ForbiddenException.class, () -> oppfolgingController.hentOppfolgingsPeriode(uuid));
    }

    @Test
    void avsluttOppfolgingHvisIserv() {
        mockAuthOk();
        startOppfolging();
        ApiResult<Decision> permit = ApiResult.Companion.success(Decision.Permit.INSTANCE);
        // Tester ikke tilgang
        doReturn(permit).when(poaoTilgangClient).evaluatePolicy(any());
        // ISERV i arena, ingen ytelser i arena, ingen aktive tiltak hos komet.
        when(veilarbarenaClient.getArenaOppfolgingsstatus(FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolgingsStatus().setFormidlingsgruppe("ISERV")));
        when(veilarbarenaClient.hentOppfolgingsbruker(FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolgingsBruker().setFormidlingsgruppekode("ISERV")));
        when(arenaYtelserService.harPagaendeYtelse(FNR)).thenReturn(false);
        when(amtDeltakerClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(false);

        AvslutningStatus avslutningStatus = oppfolgingController.hentAvslutningStatus(FNR);
        assertTrue(avslutningStatus.kanAvslutte);
        var navIdent = new NavIdent("Z151515");
        var begrunnelse = "Har fått jobb";
        var dto = new AvsluttOppfolgingV2Request();
        dto.setBegrunnelse(begrunnelse);
        dto.setVeilederId(navIdent);
        dto.setFnr(FNR);
        oppfolgingV2Controller.avsluttOppfolging(dto);
        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID);
        assertEquals(1, perioder.size());
        assertEquals(navIdent.get(), perioder.getFirst().getAvsluttetAv());
        assertEquals(begrunnelse, perioder.getFirst().getBegrunnelse());
    }

    @Test
    void ikkeAvsluttOppfolgingHvisIservOgAktivtTiltak() {
        mockAuthOk();
        var startPeriode = startOppfolging();
        ApiResult<Decision> permit = ApiResult.Companion.success(Decision.Permit.INSTANCE);
        // Tester ikke tilgang
        doReturn(permit).when(poaoTilgangClient).evaluatePolicy(any());
        // ISERV i arena, ingen ytelser i arena, men aktive tiltak hos komet.
        when(veilarbarenaClient.hentOppfolgingsbruker(FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolgingsBruker().setFormidlingsgruppekode("ISERV")));
        when(arenaYtelserService.harPagaendeYtelse(FNR)).thenReturn(false);
        when(amtDeltakerClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(true);

        var dto = new AvsluttOppfolgingV2Request();
        dto.setBegrunnelse("Begrunnelse");
        dto.setVeilederId(new NavIdent("Z151515"));
        dto.setFnr(FNR);
        var avslutningStatus = oppfolgingV2Controller.avsluttOppfolging(dto);
        assertEquals(avslutningStatus.getStatusCode(), HttpStatusCode.valueOf(204));
        OppfolgingPeriodeMinimalDTO periode = oppfolgingController.hentOppfolgingsPeriode(startPeriode.get(0).uuid.toString());
        assertNull(periode.getSluttDato());
    }

    private List<OppfolgingPeriodeDTO> startOppfolging() {
        mockSystemBruker();
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR);
        var perioder = oppfolgingController.hentOppfolgingsperioder(FNR);
        mockAuthOk();
        return perioder;
    }

    private void mockSystemBruker() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:veilarbregistrering")
                .claim("roles", Collections.singletonList("access_as_application"))
                .claim("sub", sub.toString())
                .claim("oid", sub.toString())
                .build();
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authContextHolder.erSystemBruker()).thenReturn(true);
        when(authContextHolder.erInternBruker()).thenReturn(false);
        when(authContextHolder.erEksternBruker()).thenReturn(false);
    }

    private void mockAuthOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:veilarbregistrering")
                .claim("sub", sub.toString())
                .claim("oid", veilederUUID.toString())
                .claim("NAVident", veilederIdent)
                .build();
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        String token = "token";
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(token));
        when(authContextHolder.getUid()).thenReturn(Optional.of(veilederUUID.toString()));
        when(authContextHolder.erSystemBruker()).thenReturn(false);
        when(authContextHolder.erInternBruker()).thenReturn(true);
        when(authContextHolder.erEksternBruker()).thenReturn(false);
        when(aktorOppslagClient.hentAktorId(FNR)).thenReturn(AKTOR_ID);
        when(aktorOppslagClient.hentFnr(AKTOR_ID)).thenReturn(FNR);
    }

}
