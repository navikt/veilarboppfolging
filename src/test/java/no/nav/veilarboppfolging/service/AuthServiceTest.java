package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import lombok.SneakyThrows;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
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
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import no.nav.veilarboppfolging.utils.auth.PolicyInputMatcher;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.test.auth.AuthTestUtils.TEST_AUDIENCE;
import static no.nav.veilarboppfolging.test.TestData.*;
import static no.nav.veilarboppfolging.utils.auth.AllowListApplicationName.VEILARBAKTIVITET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class AuthServiceTest {

    @Mock
    private AuthContextHolder authContextHolder;
    @Mock
    private AktorOppslagClient aktorOppslagClient;
    @Mock
    private AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;
    @Mock
    private MachineToMachineTokenClient machineToMachineTokenClient;
    @Mock
    private EnvironmentProperties environmentProperties;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private PoaoTilgangClient poaoTilgangClient;

    private AuthService authService;

    @Test
    void sjekkAtSystembrukerErIAllowedList__skal_ikke_kaste_exception_hvis_allowed() {
        setupAuthService();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        assertDoesNotThrow(() -> authService.sjekkAtApplikasjonErIAllowList(List.of("test_app")));
    }

    @Test
    void sjekkAtSystembrukerErIAllowedList__skal_kaste_exception_hvis_ikke_allowed() {
        setupAuthService();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));

        assertThrows(ForbiddenException.class, () -> authService.sjekkAtApplikasjonErIAllowList(List.of("some-id")));
    }

    @Test
    void brukerInnloggetMedLevel3SkalFaTilgangTilSegSelv_sjekkTilgangTilPersonMedNiva3() {
        setupAuthService();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level3")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("12345678910"));

        assertDoesNotThrow(() -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    void brukerInnloggetMedLevel4SkalFaTilgangTilSegSelv_sjekkTilgangTilPersonMedNiva3() {
        setupAuthService();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("12345678910"));

        assertDoesNotThrow(() -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    void brukerInnloggetMedLevel4SkalIkkeFaTilgangTilAndre_sjekkTilgangTilPersonMedNiva3() {
        setupAuthService();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", "12345678910")
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(Fnr.of("23456789101"));

        assertThrows(ForbiddenException.class, () -> authService.sjekkTilgangTilPersonMedNiva3(aktorId));
    }

    @Test
    void veilederHarLeseEllerSkrivetilgangPaAktorId_sjekktilgang() {
        setupAuthService();
        UUID uuid = UUID.randomUUID();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        AktorId aktorId = AktorId.of("123");
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
    void veilederHarIkkeLeseEllerSkrivetilgangPaAktorId_sjekktilgang() {
        setupAuthService();
        UUID uuid = UUID.randomUUID();
        Fnr fnr = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnr);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.LESE, fnr.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                uuid, TilgangType.SKRIVE, fnr.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));

        assertThrows(ForbiddenException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ForbiddenException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }


    @Test
    void EksternBrukerHarTilgangPaAktorId_sjekktilgang() {
        setupAuthService();
        Fnr fnr = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("pid", fnr.get())
                .claim("acr", "Level4")
                .build();

        AktorId aktorId = AktorId.of("123");
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
    void eksternBrukerHarIkkeTilgangPaAktorId_sjekktilgang() {
        setupAuthService();
        Fnr fnrInnloggetBruker = Fnr.of("23456789101");
        Fnr fnrDestination = Fnr.of("12345678910");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level4")
                .claim("pid", fnrInnloggetBruker)
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnrDestination);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrDestination.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrDestination.get()))).thenReturn(new ApiResult<>(null, new Decision.Deny("", "")));

        assertThrows(ForbiddenException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ForbiddenException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    void eksternBrukerMedLevel3HarIkkeTilgangPaAktorId_sjekktilgang_() {
        setupAuthService();
        Fnr fnrInnloggetBruker = Fnr.of("23456789101");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("acr", "Level3")
                .claim("pid", fnrInnloggetBruker)
                .build();

        AktorId aktorId = AktorId.of("123");
        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.getFnrOrThrow(aktorId)).thenReturn(fnrInnloggetBruker);
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrInnloggetBruker.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        when(poaoTilgangClient.evaluatePolicy(new EksternBrukerTilgangTilEksternBrukerPolicyInput(
                fnrInnloggetBruker.get(), fnrInnloggetBruker.get()))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));

        assertThrows(ForbiddenException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ForbiddenException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    void harIkkeRolleSkalFaPermissionDenied_sjekkTilgang() {
        setupAuthService();
        AktorId aktorId = AktorId.of("123");
        assertThrows(ForbiddenException.class, () -> authService.sjekkLesetilgangMedAktorId(aktorId));
        assertThrows(ForbiddenException.class, () -> authService.sjekkSkrivetilgangMedAktorId(aktorId));
    }

    @Test
    void eksternBrukerSkalFaDeny_harTilgangTilEnhet() {
        setupAuthService();
        EnhetId enhetId = EnhetId.of("1201");
        when(authService.erEksternBruker()).thenReturn(true);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(""));

        assertFalse(authService.harTilgangTilEnhet(enhetId.get()));
    }

    @Test
    void brukerMedSystemRolleEllerUkjentRolleSkalFaDeny_harTilgangTilEnhet() {
        setupAuthService();
        EnhetId enhetId = EnhetId.of("1201");
        when(authService.erSystemBruker()).thenReturn(true);
        when(authContextHolder.getIdTokenString()).thenReturn(Optional.of(""));

        assertFalse(authService.harTilgangTilEnhet(enhetId.get()));
    }


    @Test
    void internBruker_harTilgangTilEnhet() {
        setupAuthService();
        UUID uuid = UUID.randomUUID();
        EnhetId enhetId = EnhetId.of("1201");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("microsoftonline.com")
                .claim("azp_name", "cluster:team:test_app")
                .claim("oid", uuid.toString())
                .build();

        when(authContextHolder.getIdTokenClaims()).thenReturn(Optional.of(claims));
        when(authService.erInternBruker()).thenReturn(true);
        when(poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetPolicyInput(uuid, ofNullable(enhetId.get()).orElse("")))).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        assertDoesNotThrow(() -> authService.harTilgangTilEnhet(enhetId.get()));
    }

    @SneakyThrows
    @Test
    void should_allow_system_user_if_in_allowlist_auth_service() {
        setupSystemUserAuthOk();
        setupAuthService();

        List<String> allowList = List.of(VEILARBAKTIVITET);
        assertDoesNotThrow(() -> authService.authorizeRequest(TEST_FNR_2, allowList));
    }

    @SneakyThrows
    @Test
    void should_not_allow_system_user_if_not_in_allowlist_auth_service() {
        setupSystemUserNotInAllowList();
        setupAuthService();

        List<String> allowList = List.of(VEILARBAKTIVITET);
        assertThrows(ForbiddenException.class, () -> authService.authorizeRequest(TEST_FNR_2, allowList));
    }

    @SneakyThrows
    @Test
    void should_allow_external_user_if_access_self_auth_service() {
        setUpExternalUserAuthOk();
        setupAuthService();

        assertDoesNotThrow(() -> authService.authorizeRequest(TEST_FNR_2, emptyList()));
    }

    @SneakyThrows
    @Test
    void should_not_allow_external_user_if_access_other_auth_service() {
        setUpExternalUserAuthOk();
        setupAuthService();

        assertThrows(ForbiddenException.class, () -> authService.authorizeRequest(Fnr.of("11120231920"), emptyList()));
    }

    @SneakyThrows
    @Test
    void should_allow_internal_user_if_access_ok_auth_service() {
        setupInternalUserAuthOk();
        setupAuthService();

        when(aktorOppslagClient.hentAktorId(TEST_FNR_2)).thenReturn(TEST_AKTOR_ID_3);
        assertDoesNotThrow(() -> authService.authorizeRequest(TEST_FNR_2, emptyList()));
    }

    @SneakyThrows
    @Test
    void external_user_can_not_query_using_aktorid_auth_service() {
        setUpExternalUserAuthOk();
        setupAuthService();

        assertThrows(ForbiddenException.class, () -> authService.authorizeRequest(TEST_AKTOR_ID_3, emptyList()));
    }

    @SneakyThrows
    @Test
    void internal_user_can_query_using_aktorid_auth_service() {
        setupInternalUserAuthOk();
        setupAuthService();

        assertDoesNotThrow(() -> authService.authorizeRequest(TEST_AKTOR_ID_3, emptyList()));
    }

    private void setupAuthService() {
        this.authService = new AuthService(
                authContextHolder,
                aktorOppslagClient,
                azureAdOnBehalfOfTokenClient,
                machineToMachineTokenClient,
                environmentProperties,
                auditLogger,
                poaoTilgangClient
        );
    }

    private void setupSystemUserAuthOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TEST_AZURE_ISSUER)
                .claim("azp_name", "cluster:team:veilarbaktivitet")
                .claim("roles", Collections.singletonList("access_as_application"))
                .build();

        AuthContext authContext = new AuthContext(
                UserRole.SYSTEM,
                new PlainJWT(claims)
        );

        this.authContextHolder = AuthContextHolderThreadLocal.instance();
        this.authContextHolder.setContext(authContext);
    }

    private void setupSystemUserNotInAllowList() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TEST_AZURE_ISSUER)
                .claim("azp_name", "cluster:team:veilarbhacker")
                .claim("roles", Collections.singletonList("access_as_application"))
                .build();

        AuthContext authContext = new AuthContext(
                UserRole.SYSTEM,
                new PlainJWT(claims)
        );

        this.authContextHolder = AuthContextHolderThreadLocal.instance();
        this.authContextHolder.setContext(authContext);
    }

    private void setUpExternalUserAuthOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(TEST_FNR_2.get())
                .claim("pid", TEST_FNR_2.get())
                .claim("acr", "Level4")
                .claim("client_id", "test_client_id")
                .audience(TEST_AUDIENCE)
                .issuer(TEST_TOKENDINGS_ISSUER)
                .build();

        AuthContext authContext = new AuthContext(
                UserRole.valueOf("EKSTERN"),
                new PlainJWT(claims)
        );

        this.authContextHolder = AuthContextHolderThreadLocal.instance();
        this.authContextHolder.setContext(authContext);
    }

    private void setupInternalUserAuthOk() {
        UUID uuid = UUID.randomUUID();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(TEST_NAV_IDENT_2.get())
                .issuer(TEST_AZURE_ISSUER)
                .audience(TEST_AUDIENCE)
                .claim("oid", uuid.toString())
                .claim(AAD_NAV_IDENT_CLAIM, TEST_NAV_IDENT_2.get())
                .claim("azp_name", "test_client_id")
                .build();

        AuthContext authContext = new AuthContext(
                UserRole.valueOf("INTERN"),
                new PlainJWT(claims)
        );

        this.authContextHolder = AuthContextHolderThreadLocal.instance();
        this.authContextHolder.setContext(authContext);

        when(aktorOppslagClient.hentFnr(TEST_AKTOR_ID_3)).thenReturn(TEST_FNR_2);

        Decision decision = Decision.Permit.INSTANCE;
        doReturn(new ApiResult<>(null, decision)).when(poaoTilgangClient).evaluatePolicy(argThat(new PolicyInputMatcher(uuid, TEST_FNR_2.get())));
    }
}
