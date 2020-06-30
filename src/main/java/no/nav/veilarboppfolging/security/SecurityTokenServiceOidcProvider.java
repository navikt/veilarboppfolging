package no.nav.veilarboppfolging.security;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.sbl.rest.RestUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

public class SecurityTokenServiceOidcProvider implements OidcProvider {

    private final String expectedIssuer;
    private final JsonWebKeyCache keyCache;

    public SecurityTokenServiceOidcProvider(SecurityTokenServiceOidcProviderConfig securityTokenServiceOidcProviderConfig) {
        Configuration configuration = RestUtils.withClient(c -> c.target(securityTokenServiceOidcProviderConfig.discoveryUrl).request().get(Configuration.class));
        this.expectedIssuer = assertNotNullOrEmpty(configuration.issuer);
        this.keyCache = new JsonWebKeyCache(configuration.jwks_uri, true);
    }

    @Override
    public Optional<String> getToken(HttpServletRequest httpServletRequest) {
        String headerValue = httpServletRequest.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

    @Override
    public Optional<String> getRefreshToken(HttpServletRequest httpServletRequest) {
        return Optional.empty(); // støtter ikke refresh
    }

    @Override
    public OidcCredential getFreshToken(String refreshToken, String requestToken) {
        throw new IllegalStateException("not supported"); // støtter ikke refresh
    }

    @Override
    public Optional<Key> getVerificationKey(JwtHeader header, CacheMissAction cacheMissAction) {
        return keyCache.getVerificationKey(header, cacheMissAction);
    }

    @Override
    public String getExpectedIssuer() {
        return expectedIssuer;
    }

    @Override
    public String getExpectedAudience(String token) {
        return null;  // vi godtar alle audiences for intern oidc.
    }

    @Override
    public IdentType getIdentType(String token) {
        return IdentType.InternBruker;
    }

    @SuppressWarnings("unused")
    private static class Configuration {
        private String issuer;
        private String jwks_uri;
    }

}
