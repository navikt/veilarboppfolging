package no.nav.veilarboppfolging.utils.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import lombok.SneakyThrows;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.audit_log.log.AuditLogger;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
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

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.test.auth.AuthTestUtils.TEST_AUDIENCE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AuthorizationAnnotationHandlerTest {

    private final static Fnr FNR = Fnr.of("12345678901");
    private final static NavIdent VEILEDER = NavIdent.of("Z123456");

    private final static AktorId AKTOR_ID = AktorId.of("3409823");

    private final static String TOKENDINGS_ISSUER="https://tokendings";
    private final static String AZURE_ISSUER="microsoftonline.com";

    @Mock
    private Pep veilarbPep;
    @Mock
    private AktorOppslagClient aktorOppslagClient;
    @Mock
    private AuditLogger auditLogger;

    private AuthContextHolder authContextHolder;

    private AuthorizationAnnotationHandler annotationHandler;


    @SneakyThrows
    @Test
    void should_allow_system_user_if_in_allowlist() {
        setupSystemUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", FNR.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    @SneakyThrows
    @Test
    void should_not_allow_system_user_if_not_in_allowlist() {
        setupSystemUserNotInAllowList();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", FNR.get());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SneakyThrows
    @Test
    void should_allow_external_user_if_access_self() {
        setUpExternalUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", FNR.get());
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

        when(aktorOppslagClient.hentAktorId(FNR)).thenReturn(AKTOR_ID);

        Method method = OppfolgingV2Controller.class.getMethod("hentGjeldendePeriode", Fnr.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fnr", FNR.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    @SneakyThrows
    @Test
    void external_user_can_not_query_using_aktorid() {
        setUpExternalUserAuthOk();
        Method method = OppfolgingV2Controller.class.getMethod("hentOppfolgingsperioder", AktorId.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("aktorId", AKTOR_ID.get());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SneakyThrows
    @Test
    void internal_user_can_query_using_aktorid() {
        setupInternalUserAuthOk();

        Method method = OppfolgingV2Controller.class.getMethod("hentOppfolgingsperioder", AktorId.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("aktorId", AKTOR_ID.get());
        assertDoesNotThrow(() -> annotationHandler.doAuthorizationCheckIfTagged(method, request));
    }

    private void setupServices() {
        AuthService authService = new AuthService(authContextHolder, veilarbPep, aktorOppslagClient, null, null, null, auditLogger);
        annotationHandler = new AuthorizationAnnotationHandler(authService);
    }

    private void setupSystemUserAuthOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AZURE_ISSUER)
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
                .issuer(AZURE_ISSUER)
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
                .subject(FNR.get())
                .claim("pid", FNR.get())
                .claim("acr", "Level4")
                .claim("client_id", "test_client_id")
                .audience(TEST_AUDIENCE)
                .issuer(TOKENDINGS_ISSUER)
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

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(VEILEDER.get())
                .issuer(AZURE_ISSUER)
                .audience(TEST_AUDIENCE)
                .claim(AAD_NAV_IDENT_CLAIM, VEILEDER.get())
                .claim("azp_name", "test_client_id")
                .build();

        AuthContext authContext = new AuthContext(
                UserRole.valueOf("INTERN"),
                new PlainJWT(claims)
        );

        authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(authContext);

        setupServices();

        when(veilarbPep.harVeilederTilgangTilPerson(VEILEDER, ActionId.READ, AKTOR_ID)).thenReturn(true);
    }
}