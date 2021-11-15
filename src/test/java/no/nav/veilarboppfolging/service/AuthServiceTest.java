package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.utils.Credentials;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    private final AuthContextHolder authContextHolder = mock(AuthContextHolder.class);

    private final Pep veilarbPep = mock(Pep.class);

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    private final AktorregisterClient aktorregisterClient = mock(AktorregisterClient.class);

    private final Credentials serviceUserCredentials = mock(Credentials.class);

    private AuthService authService = new AuthService(
            authContextHolder,
            veilarbPep,
            aktorOppslagClient,
            aktorregisterClient,
            serviceUserCredentials
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
    public void sjekkAtSystembrukerErWhitelistet__skal_ikke_kaste_exception_hvis_whitelistet() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("azp", "test")
                .build();

        when(authContextHolder.requireIdTokenClaims()).thenReturn(claims);

        assertDoesNotThrow(() -> authService.sjekkAtSystembrukerErWhitelistet("test"));
    }

    @Test
    public void sjekkAtSystembrukerErWhitelistet__skal_kaste_exception_hvis_ikke_whitelistet() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("azp", "test")
                .build();

        when(authContextHolder.requireIdTokenClaims()).thenReturn(claims);

        assertThrows(ResponseStatusException.class, () -> authService.sjekkAtSystembrukerErWhitelistet("some-id"));
    }

}
