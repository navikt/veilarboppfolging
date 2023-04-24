package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.abac.Pep;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.audit_log.log.AuditLoggerImpl;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.Credentials;
import no.nav.poao_tilgang.client.*;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    private final AuthContextHolder authContextHolder = mock(AuthContextHolder.class);

    private final Pep veilarbPep = mock(Pep.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final Credentials serviceUserCredentials = mock(Credentials.class);

    private final AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient = mock(AzureAdOnBehalfOfTokenClient.class);
    private final MachineToMachineTokenClient machineToMachineTokenClient = mock(MachineToMachineTokenClient.class);

    private final EnvironmentProperties environmentProperties = mock(EnvironmentProperties.class);

    private final AuditLogger auditLogger = mock(AuditLoggerImpl.class);

    private final UnleashService unleashService = mock(UnleashService.class);

    private final PoaoTilgangClient poaoTilgangClient = mock(PoaoTilgangClient.class);

    private AuthService authService = new AuthService(
            authContextHolder,
            veilarbPep,
            aktorOppslagClient,
            azureAdOnBehalfOfTokenClient,
            machineToMachineTokenClient,
            environmentProperties,
            auditLogger,
            poaoTilgangClient,
            unleashService
    );

    @Test
    public void skalVereEnAv__skal_sjekke_at_rolle_stemmer() {
        when(authContextHolder.requireRole()).thenReturn(UserRole.SYSTEM);
        assertDoesNotThrow(() -> authService.skalVereEnAv(List.of(UserRole.INTERN, UserRole.SYSTEM)));
    }

    @Test
    public void skalVereEnAv__skal_feile_hvis_rolle_ikke_() {
        when(authContextHolder.requireRole()).thenReturn(UserRole.SYSTEM);
        assertThrows(ResponseStatusException.class, () -> authService.skalVereEnAv(List.of(UserRole.INTERN)));
    }

    @Test
    public void skalVereEnAv__skal_feile_hvis_rolle_mangler() {
        when(authContextHolder.getRole()).thenReturn(Optional.empty());
        when(authContextHolder.requireRole()).thenCallRealMethod();
        assertThrows(IllegalStateException.class, () -> authService.skalVereEnAv(List.of(UserRole.INTERN)));
    }

    @Test
    public void sjekkAtSystembrukerErIAllowedList__skal_ikke_kaste_exception_hvis_allowed() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        assertDoesNotThrow(() -> authService.sjekkAtApplikasjonErIAllowList(List.of("test_app")));
    }

    @Test
    public void sjekkAtSystembrukerErIAllowedList__skal_kaste_exception_hvis_ikke_allowed() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        assertThrows(ResponseStatusException.class, () -> authService.sjekkAtApplikasjonErIAllowList(List.of("some-id")));
    }

    @Test
    public void brukerInnloggetMedLevel3SkalFaTilgangTilSegSelv_sjekkTilgangTilPersonMedNiva3() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level3")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("12345678910"));

        assertDoesNotThrow(() -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    public void brukerInnloggetMedLevel4SkalFaTilgangTilSegSelv_sjekkTilgangTilPersonMedNiva3() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("12345678910"));

        assertDoesNotThrow(() -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    public void brukerInnloggetMedLevel4SkalIkkeFaTilgangTilAndre_sjekkTilgangTilPersonMedNiva3() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("23456789101"));

        assertThrows(ResponseStatusException.class, () -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    public void veilederHarLeseEllerSkrivetilgangPaAktorId_sjekktilgang() {
        UUID uuid = UUID.randomUUID();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("23456789101"));
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.LESE, "23456789101"))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.SKRIVE, "23456789101"))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));

        assertDoesNotThrow(() -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertDoesNotThrow(() -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    public void veilederHarIkkeLeseEllerSkrivetilgangPaAktorId_sjekktilgang() {
        UUID uuid = UUID.randomUUID();
        Fnr fnr = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnr);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.LESE, fnr.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.SKRIVE, fnr.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));

        assertThrows(ResponseStatusException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ResponseStatusException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }


    @Test
    public void EksternBrukerHarTilgangPaAktorId_sjekktilgang() {
        Fnr fnr = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("pid", fnr.get())
                .claim("acr", "Level4")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnr);
        when(authService.erEksternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnr.get(), fnr.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnr.get(), fnr.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));

        assertDoesNotThrow(() -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertDoesNotThrow(() -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    public void eksternBrukerHarIkkeTilgangPaAktorId_sjekktilgang() {
        Fnr fnrInnloggetBruker = Fnr.of("23456789101");
        Fnr fnrDestination = Fnr.of("12345678910");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", fnrInnloggetBruker)
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnrDestination);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrDestination.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrDestination.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));

        assertThrows(ResponseStatusException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ResponseStatusException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    public void eksternBrukerMedLevel3HarIkkeTilgangPaAktorId_sjekktilgang_() {
        Fnr fnrInnloggetBruker = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level3")
                .claim("pid", fnrInnloggetBruker)
                .build();

        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnrInnloggetBruker);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrInnloggetBruker.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrInnloggetBruker.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));

        assertThrows(ResponseStatusException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ResponseStatusException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    public void harIkkeRolleSkalFaPermissionDenied_sjekkTilgang() {
        AktorId aktorId = AktorId.of("123");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ResponseStatusException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    public void eksternBrukerSkalFaPermitHvisAbacGirPermit_harTilgangTilEnhet() {
        EnhetId enhetId = EnhetId.of("1201");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authService.erEksternBruker()).thenReturn(true);
        when(veilarbPep.harTilgangTilEnhet("", ofNullable(enhetId.get()).map(EnhetId::of).orElse(EnhetId.of("")))).thenReturn(true);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(""));

        assertTrue(authService.harTilgangTilEnhet(enhetId.get()));
    }

    @Test
    public void eksternBrukerSkalFaDeny_harTilgangTilEnhet() {
        EnhetId enhetId = EnhetId.of("1201");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authService.erEksternBruker()).thenReturn(true);
        when(veilarbPep.harTilgangTilEnhet("", ofNullable(enhetId.get()).map(EnhetId::of).orElse(EnhetId.of("")))).thenReturn(false);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(""));

        assertFalse(authService.harTilgangTilEnhet(enhetId.get()));
    }

    @Test
    public void brukerMedSystemRolleEllerUkjentRolleSkalFaDeny_harTilgangTilEnhet() {
        EnhetId enhetId = EnhetId.of("1201");
        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authService.erSystemBruker()).thenReturn(true);
        when(veilarbPep.harTilgangTilEnhet("", ofNullable(enhetId.get()).map(EnhetId::of).orElse(EnhetId.of("")))).thenReturn(false);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(""));

        assertFalse(authService.harTilgangTilEnhet(enhetId.get()));
    }


    @Test
    public void internBruker_harTilgangTilEnhet() {
        UUID uuid = UUID.randomUUID();
        EnhetId enhetId = EnhetId.of("1201");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        when(unleashService.skalBrukePoaoTilgang()).thenReturn(true);
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetPolicyInput(uuid,  ofNullable(enhetId.get()).orElse("")))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        assertDoesNotThrow(() -> authService.harTilgangTilEnhet(enhetId.get()));
    }

}
