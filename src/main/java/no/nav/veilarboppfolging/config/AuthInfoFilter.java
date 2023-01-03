package no.nav.veilarboppfolging.config;

import com.nimbusds.jwt.JWTClaimsSet;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarboppfolging.service.AuthService;
import org.checkerframework.checker.units.qual.A;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;


@RequiredArgsConstructor
public class AuthInfoFilter implements Filter {

    private final MeterRegistry meterRegistry;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        AuthContextHolder authContextHolder = AuthContextHolderThreadLocal.instance();

        String consumerId = request.getHeader(NAV_CONSUMER_ID_HEADER_NAME);
        if (consumerId == null) {
            consumerId = "UKJENT";
        }

        String userRole = authContextHolder.getRole().map(UserRole::name).orElse("UKJENT");
        var claims = authContextHolder.getIdTokenClaims();
        String tokenIssuer = claims.map(JWTClaimsSet::getIssuer).orElse("");

        String tokenType;
        if (AuthService.isAzure(claims)) {
            tokenType = "AAD";
        } else if (tokenIssuer.contains("difi.no")) {
            tokenType = "IDPORTEN";
        } else if (AuthService.isTokenX(claims)) {
            tokenType = "TOKENX";
        } else if (tokenIssuer.contains("security-token-service")) {
            tokenType = "STS";
        } else {
            tokenType = "UKJENT";
        }

        meterRegistry.counter(
                "auth_info_token_type",
                List.of(
                        Tag.of("type", tokenType),
                        Tag.of("consumer_id", consumerId),
                        Tag.of("user_role", userRole)
                )
        ).increment();

        chain.doFilter(servletRequest, response);
    }
}
