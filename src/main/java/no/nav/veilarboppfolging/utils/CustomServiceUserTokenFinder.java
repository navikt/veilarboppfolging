package no.nav.veilarboppfolging.utils;

import no.nav.common.auth.utils.TokenFinder;
import no.nav.common.auth.utils.TokenUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class CustomServiceUserTokenFinder implements TokenFinder {

    // Veilarbregistrering bruker en gammel måte å sende tokens på som vi må støtte inntil videre
    private Optional<String> getSystemAuthorizationToken(HttpServletRequest request) {
        String headerValue = request.getHeader("SystemAuthorization");
        return Optional.ofNullable(headerValue);
    }

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        Optional<String> maybeToken = TokenUtils.getTokenFromHeader(request)
                .or(() -> getSystemAuthorizationToken(request));

        if (maybeToken.isPresent() && TokenUtils.isServiceUserToken(maybeToken.get())) {
            return maybeToken;
        }

        return Optional.empty();
    }

}