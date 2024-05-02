package no.nav.veilarboppfolging.utils.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import lombok.SneakyThrows;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarboppfolging.controller.v2.OppfolgingV2Controller;
import no.nav.veilarboppfolging.service.AuthService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.test.auth.AuthTestUtils.TEST_AUDIENCE;
import static no.nav.veilarboppfolging.test.TestData.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationAnnotationHandlerTest {

    @Mock
    private AktorOppslagClient aktorOppslagClient;
    @Mock
    private AuditLogger auditLogger;

    private AuthContextHolder authContextHolder;

    private AuthorizationAnnotationHandler annotationHandler;

    private AuthService authService;

    @Mock
    private PoaoTilgangClient poaoTilgangClient;

    @SneakyThrows
    @Test
    void should_allow_system_user_if_in_allowlist() {
        setupSystemUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", TEST_FNR_2.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    @SneakyThrows
    @Test
    void should_not_allow_system_user_if_not_in_allowlist() {
        setupSystemUserNotInAllowList();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", TEST_FNR_2.get());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SneakyThrows
    @Test
    void should_allow_external_user_if_access_self() {
        setUpExternalUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", TEST_FNR_2.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    @SneakyThrows
    @Test
    void should_not_allow_external_user_if_access_other() {
        setUpExternalUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        String otherFnr = "11120231920";
        request.setParameter("fnr", otherFnr);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SneakyThrows
    @Test
    void should_allow_internal_user_if_access_ok() {
        setupInternalUserAuthOk();
        when(aktorOppslagClient.hentAktorId(TEST_FNR_2)).thenReturn(TEST_AKTOR_ID_3);

        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", TEST_FNR_2.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    @SneakyThrows
    @Test
    void external_user_can_not_query_using_aktorid() {
        setUpExternalUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentOppfolgingsperioder", AktorId.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("aktorId", TEST_AKTOR_ID_3.get());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SneakyThrows
    @Test
    void internal_user_can_query_using_aktorid() {
        setupInternalUserAuthOk();

        Method method = OppfolgingV2Controller.class.getMethod("hentOppfolgingsperioder", AktorId.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("aktorId", TEST_AKTOR_ID_3.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    private void setupServices() {
        AuthService authService = new AuthService(authContextHolder, aktorOppslagClient, null, null, null, auditLogger, poaoTilgangClient);
        this.authService = authService;
        annotationHandler = new AuthorizationAnnotationHandler(authService);
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

        authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(authContext);
        setupServices();
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

        authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(authContext);
        setupServices();
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

        authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(authContext);
        setupServices();

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

        authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(authContext);

        setupServices();

        when(aktorOppslagClient.hentFnr(TEST_AKTOR_ID_3)).thenReturn(TEST_FNR_2);

        Decision decision = Decision.Permit.INSTANCE;
        doReturn(new ApiResult<Decision>(null, decision)).when(poaoTilgangClient).evaluatePolicy(argThat(new PolicyInputMatcher(uuid, TEST_FNR_2.get())));
    }
}
