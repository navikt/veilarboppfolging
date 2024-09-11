package no.nav.veilarboppfolging.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.client.amttiltak.AmtTiltakClient;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.controller.request.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.controller.response.AvslutningStatus;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.service.ArenaYtelserService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class OppfolgingControllerIntegrationTest extends IntegrationTest {

    private final static Fnr FNR = Fnr.of("123");
    private final static AktorId AKTOR_ID = AktorId.of("3409823");
    private final static UUID veilederUUID = UUID.randomUUID();
    private final static String veilederIdent = "Z999999";
    private final static UUID sub = UUID.randomUUID();

    @Autowired
    AktorOppslagClient aktorOppslagClient;

    @Autowired
    AuthService authService;

    @MockBean
    ArenaYtelserService arenaYtelserService;

    @MockBean
    VeilarbarenaClient veilarbarenaClient;

    @Autowired
    OppfolgingController oppfolgingController;

    @Autowired
    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Autowired
    SystemOppfolgingController systemOppfolgingController;

    @Autowired
    PoaoTilgangClient poaoTilgangClient;

    @Autowired
    AmtTiltakClient amtTiltakClient;

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

        assertThrows(ResponseStatusException.class, () -> oppfolgingController.hentOppfolgingsPeriode(uuid));
    }

    @Test
    void avsluttOppfolgingHvisIserv() {
        mockAuthOk();
        var startPeriode = startOppfolging();
        ApiResult<Decision> permit = ApiResult.Companion.success(Decision.Permit.INSTANCE);
        // Tester ikke tilgang
        doReturn(permit).when(poaoTilgangClient).evaluatePolicy(any());
        // ISERV i arena, ingen ytelser i arena, ingen aktive tiltak hos komet.
        when(veilarbarenaClient.getArenaOppfolgingsstatus(FNR)).thenReturn(Optional.of(new ArenaOppfolging().setFormidlingsgruppe("ISERV")));
        when(veilarbarenaClient.hentOppfolgingsbruker(FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolging().setFormidlingsgruppekode("ISERV")));
        when(arenaYtelserService.harPagaendeYtelse(FNR)).thenReturn(false);
        when(amtTiltakClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(false);

        AvslutningStatus avslutningStatus = oppfolgingController.hentAvslutningStatus(FNR);
        assertTrue(avslutningStatus.kanAvslutte);
        oppfolgingController.avsluttOppfolging(new VeilederBegrunnelseDTO(), FNR);
        OppfolgingPeriodeMinimalDTO periode = oppfolgingController.hentOppfolgingsPeriode(startPeriode.get(0).uuid.toString());
        assertNotNull(periode.getSluttDato());
    }

    @Test
    void ikkeAvsluttOppfolgingHvisIservOgAktivtTiltak() {
        mockAuthOk();
        var startPeriode = startOppfolging();
        ApiResult<Decision> permit = ApiResult.Companion.success(Decision.Permit.INSTANCE);
        // Tester ikke tilgang
        doReturn(permit).when(poaoTilgangClient).evaluatePolicy(any());
        // ISERV i arena, ingen ytelser i arena, men aktive tiltak hos komet.
        when(veilarbarenaClient.getArenaOppfolgingsstatus(FNR)).thenReturn(Optional.of(new ArenaOppfolging().setFormidlingsgruppe("ISERV")));
        when(arenaYtelserService.harPagaendeYtelse(FNR)).thenReturn(false);
        when(amtTiltakClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(true);

        AvslutningStatus avslutningStatus = oppfolgingController.avsluttOppfolging(new VeilederBegrunnelseDTO(), FNR);
        assertFalse(avslutningStatus.kanAvslutte);
        assertTrue(avslutningStatus.harAktiveTiltaksdeltakelser);
        OppfolgingPeriodeMinimalDTO periode = oppfolgingController.hentOppfolgingsPeriode(startPeriode.get(0).uuid.toString());
        assertNull(periode.getSluttDato());
    }

    private List<OppfolgingPeriodeDTO> startOppfolging() {
        mockSystemBruker();
        startOppfolging(AKTOR_ID, FNR);
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
